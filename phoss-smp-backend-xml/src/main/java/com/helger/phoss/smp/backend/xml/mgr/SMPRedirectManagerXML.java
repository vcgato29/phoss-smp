/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.xml.mgr;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.audit.AuditHelper;

/**
 * Manager for all {@link SMPRedirect} objects.
 *
 * @author Philip Helger
 */
public final class SMPRedirectManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPRedirect, SMPRedirect> implements
                                         ISMPRedirectManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRedirectManagerXML.class);

  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  public SMPRedirectManagerXML (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPRedirect.class, sFilename);
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _createSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
  {
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMPRedirect);
    });
    AuditHelper.onAuditCreateSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getCertificate (),
                                      aSMPRedirect.getExtensionsAsString ());
    return aSMPRedirect;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _updateSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
  {
    m_aRWLock.writeLocked ( () -> {
      internalUpdateItem (aSMPRedirect);
    });
    AuditHelper.onAuditModifySuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getCertificate (),
                                      aSMPRedirect.getExtensionsAsString ());
    return aSMPRedirect;
  }

  @Nonnull
  public ISMPRedirect createOrUpdateSMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @Nonnull @Nonempty final String sTargetHref,
                                                 @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final X509Certificate aCertificate,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPRedirect (" +
                    aServiceGroup +
                    ", " +
                    aDocumentTypeIdentifier +
                    ", " +
                    sTargetHref +
                    ", " +
                    sSubjectUniqueIdentifier +
                    ", " +
                    aCertificate +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final ISMPRedirect aOldRedirect = getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                   aDocumentTypeIdentifier);
    SMPRedirect aNewRedirect;
    if (aOldRedirect == null)
    {
      // Create new ID
      aNewRedirect = new SMPRedirect (aServiceGroup,
                                      aDocumentTypeIdentifier,
                                      sTargetHref,
                                      sSubjectUniqueIdentifier,
                                      aCertificate,
                                      sExtension);
      _createSMPRedirect (aNewRedirect);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPRedirect - success - created");

      m_aCallbacks.forEach (x -> x.onSMPRedirectCreated (aNewRedirect));
    }
    else
    {
      // Reuse old ID
      aNewRedirect = new SMPRedirect (aServiceGroup,
                                      aDocumentTypeIdentifier,
                                      sTargetHref,
                                      sSubjectUniqueIdentifier,
                                      aCertificate,
                                      sExtension);
      _updateSMPRedirect (aNewRedirect);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPRedirect - success - updated");

      m_aCallbacks.forEach (x -> x.onSMPRedirectUpdated (aNewRedirect));
    }
    return aNewRedirect;
  }

  @Nonnull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPRedirect (" + aSMPRedirect + ")");

    if (aSMPRedirect == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPRedirect - failure");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPRedirect aRealRedirect = internalDeleteItem (aSMPRedirect.getID ());
      if (aRealRedirect == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, "no-such-id", aSMPRedirect.getID ());
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("deleteSMPRedirect - failure");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aSMPRedirect));

    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPRedirect - success");
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    EChange eChange = EChange.UNCHANGED;
    for (final ISMPRedirect aRedirect : getAllSMPRedirectsOfServiceGroup (aServiceGroup.getID ()))
      eChange = eChange.or (deleteSMPRedirect (aRedirect));
    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
  {
    return getAll ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    return getAllSMPRedirectsOfServiceGroup (aServiceGroup == null ? null : aServiceGroup.getID ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    if (StringHelper.hasText (sServiceGroupID))
      findAll (x -> x.getServiceGroupID ().equals (sServiceGroupID), ret::add);
    return ret;
  }

  @Nonnegative
  public long getSMPRedirectCount ()
  {
    return size ();
  }

  @Nullable
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocTypeID == null)
      return null;

    return findFirst (x -> x.getServiceGroupID ().equals (aServiceGroup.getID ()) &&
                           aDocTypeID.hasSameContent (x.getDocumentTypeIdentifier ()));
  }
}
