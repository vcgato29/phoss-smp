/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.smlhook;

import java.net.URL;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.ws.HostnameVerifierVerifyAll;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.participant.NotFoundFault;
import com.helger.peppol.smlclient.participant.UnauthorizedFault;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;

/**
 * An implementation of the RegistrationHook that informs the SML of updates to
 * this SMP's identifiers.<br>
 * The design of this hook is very bogus! It relies on the postUpdate always
 * being called in order in this Thread.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@NotThreadSafe
public class RegistrationHookWriteToSML implements IRegistrationHook
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RegistrationHookWriteToSML.class);

  // SMP ID is static and cannot change
  private static final String SMP_ID = SMPServerConfiguration.getSMLSMPID ();

  static
  {}

  public RegistrationHookWriteToSML ()
  {}

  @Nonnull
  private static ManageParticipantIdentifierServiceCaller _createSMLCaller ()
  {
    // SML endpoint (incl. the service name)
    final ISMLInfo aSMLInfo = SMPMetaManager.getSettings ().getSMLInfo ();
    if (aSMLInfo == null)
      throw new IllegalStateException ("Failed to get SML manage participant endpoint URL");
    final URL aSMLEndpointURL = aSMLInfo.getManageParticipantIdentifierEndpointAddress ();
    final String sEndpointURL = aSMLEndpointURL.toExternalForm ();
    final String sLowerURL = sEndpointURL.toLowerCase (Locale.US);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Performing SML query to '" + sEndpointURL + "'");

    // SSL socket factory
    SSLSocketFactory aSocketFactory;
    if (sLowerURL.startsWith ("https://"))
    {
      // https connection
      if (!SMPKeyManager.isCertificateValid ())
        throw new InitializationException ("Cannot init registration hook to SML, because private key/certificate setup has errors: " +
                                           SMPKeyManager.getInitializationError ());

      try
      {
        aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init SSLContext for SML access", ex);
      }
    }
    else
    {
      // Local, http only access - no socket factory
      aSocketFactory = null;
    }

    // Hostname verifier
    HostnameVerifier aHostnameVerifier;
    if (sLowerURL.contains ("//localhost") || sLowerURL.contains ("//127.0.0.1"))
    {
      // Accept all hostnames
      aHostnameVerifier = new HostnameVerifierVerifyAll (false);
    }
    else
      aHostnameVerifier = null;

    // Build WS client
    final ManageParticipantIdentifierServiceCaller ret = new ManageParticipantIdentifierServiceCaller (aSMLEndpointURL);
    ret.setSSLSocketFactory (aSocketFactory);
    ret.setHostnameVerifier (aHostnameVerifier);
    final Integer aConnectionTimeoutMS = SMPServerConfiguration.getSMLConnectionTimeoutMS ();
    if (aConnectionTimeoutMS != null)
      ret.setConnectionTimeoutMS (aConnectionTimeoutMS.intValue ());
    final int nRequestTimeoutMS = SMPServerConfiguration.getSMLRequestTimeoutMS ();
    if (nRequestTimeoutMS >= 0)
      ret.setRequestTimeoutMS (nRequestTimeoutMS);
    return ret;
  }

  public void createServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Trying to CREATE business " + sParticipantID + " for " + SMP_ID + " in SML");

    try
    {
      // Explicit constructor call is needed here!
      _createSMLCaller ().create (SMP_ID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Succeeded in CREATE business " + sParticipantID + " in SML");
    }
    catch (final UnauthorizedFault ex)
    {
      final String sMsg = "Seems like this SMP is not registered to the SML, or you're providing invalid credentials!";
      throw new RegistrationHookException (sMsg, ex);
    }
    catch (final Exception ex)
    {
      final String sMsg = "Could not create business " + sParticipantID + " in SML";
      throw new RegistrationHookException (sMsg, ex);
    }
  }

  public void undoCreateServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    if (LOGGER.isWarnEnabled ())
      LOGGER.warn ("CREATE failed in SMP backend, so deleting again business " +
                   sParticipantID +
                   " for " +
                   SMP_ID +
                   " from SML.");

    try
    {
      // Undo create
      // Explicit constructor call is needed here!
      _createSMLCaller ().delete (SMP_ID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Succeeded in deleting again business " + sParticipantID + " from SML.");
    }
    catch (final Exception ex)
    {
      final String sMsg = "Unable to rollback create business " + sParticipantID + " in SML";
      throw new RegistrationHookException (sMsg, ex);
    }
  }

  public void deleteServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Trying to DELETE business " + sParticipantID + " for " + SMP_ID + " from SML");

    try
    {
      // Use the version with the SMP ID to be on the safe side
      // Explicit constructor call is needed here!
      _createSMLCaller ().delete (SMP_ID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Succeeded in deleting business " + sParticipantID + " from SML");
    }
    catch (final NotFoundFault ex)
    {
      final String sMsg = "The business " +
                          sParticipantID +
                          " was not present in the SML and hence could not be deleted.";
      throw new RegistrationHookException (sMsg, ex);
    }
    catch (final Exception ex)
    {
      final String sMsg = "Could not delete business " + sParticipantID + " from SML.";
      throw new RegistrationHookException (sMsg, ex);
    }
  }

  public void undoDeleteServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    if (LOGGER.isWarnEnabled ())
      LOGGER.warn ("DELETE failed in SMP backend, so creating again business " +
                   sParticipantID +
                   " for " +
                   SMP_ID +
                   " in SML.");

    try
    {
      // Undo delete
      // Explicit constructor call is needed here!
      _createSMLCaller ().create (SMP_ID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Succeeded in creating again business " + sParticipantID + " in SML.");
    }
    catch (final Exception ex)
    {
      final String sMsg = "Unable to rollback delete business " + sParticipantID + " in SML";
      throw new RegistrationHookException (sMsg, ex);
    }
  }
}
