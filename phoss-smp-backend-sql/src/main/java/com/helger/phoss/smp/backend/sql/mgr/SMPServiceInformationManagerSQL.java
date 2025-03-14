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
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.eclipse.persistence.config.CacheUsage;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.backend.sql.AbstractSMPJPAEnabledManager;
import com.helger.phoss.smp.backend.sql.model.DBEndpoint;
import com.helger.phoss.smp.backend.sql.model.DBEndpointID;
import com.helger.phoss.smp.backend.sql.model.DBProcess;
import com.helger.phoss.smp.backend.sql.model.DBProcessID;
import com.helger.phoss.smp.backend.sql.model.DBServiceGroup;
import com.helger.phoss.smp.backend.sql.model.DBServiceGroupID;
import com.helger.phoss.smp.backend.sql.model.DBServiceMetadata;
import com.helger.phoss.smp.backend.sql.model.DBServiceMetadataID;
import com.helger.phoss.smp.backend.sql.model.DBServiceMetadataRedirection;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManagerSQL extends AbstractSMPJPAEnabledManager implements
                                                   ISMPServiceInformationManager
{
  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceInformationManagerSQL (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    m_aServiceGroupMgr = aServiceGroupMgr;
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  private static void _update (@Nonnull final EntityManager aEM,
                               @Nonnull final DBServiceMetadata aDBMetadata,
                               @Nonnull final ISMPServiceInformation aServiceInfo)
  {
    // For all DB processes
    // Create a copy to avoid concurrent modification
    for (final DBProcess aDBProcess : new CommonsArrayList <> (aDBMetadata.getProcesses ()))
    {
      boolean bProcessFound = false;
      for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
        if (aDBProcess.getId ().getAsProcessIdentifier ().hasSameContent (aProcess.getProcessIdentifier ()))
        {
          bProcessFound = true;

          // Check for endpoint update
          // Create a copy to avoid concurrent modification
          for (final DBEndpoint aDBEndpoint : CollectionHelper.newList (aDBProcess.getEndpoints ()))
          {
            boolean bEndpointFound = false;
            for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
              if (aDBEndpoint.getId ().getTransportProfile ().equals (aEndpoint.getTransportProfile ()))
              {
                // And endpoint for updating was found
                bEndpointFound = true;
                aDBEndpoint.setEndpointReference (aEndpoint.getEndpointReference ());
                aDBEndpoint.setRequireBusinessLevelSignature (aEndpoint.isRequireBusinessLevelSignature ());
                aDBEndpoint.setMinimumAuthenticationLevel (aEndpoint.getMinimumAuthenticationLevel ());
                aDBEndpoint.setServiceActivationDate (aEndpoint.getServiceActivationDateTime ());
                aDBEndpoint.setServiceExpirationDate (aEndpoint.getServiceExpirationDateTime ());
                aDBEndpoint.setCertificate (aEndpoint.getCertificate ());
                aDBEndpoint.setServiceDescription (aEndpoint.getServiceDescription ());
                aDBEndpoint.setTechnicalContactUrl (aEndpoint.getTechnicalContactUrl ());
                aDBEndpoint.setTechnicalInformationUrl (aEndpoint.getTechnicalInformationUrl ());
                aDBEndpoint.setExtension (aEndpoint.getExtensionsAsString ());
                break;
              }

            if (!bEndpointFound)
            {
              // Not contained in new set
              aDBProcess.getEndpoints ().remove (aDBEndpoint);
              aEM.remove (aDBEndpoint);
            }
          }

          // Search for new endpoints
          for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
          {
            boolean bEndpointFound = false;
            for (final DBEndpoint aDBEndpoint : aDBProcess.getEndpoints ())
              if (aDBEndpoint.getId ().getTransportProfile ().equals (aEndpoint.getTransportProfile ()))
              {
                bEndpointFound = true;
                break;
              }

            if (!bEndpointFound)
            {
              // Create a new endpoint
              final DBEndpoint aDBEndpoint = new DBEndpoint (new DBEndpointID (aDBProcess.getId (),
                                                                               aEndpoint.getTransportProfile ()),
                                                             aDBProcess,
                                                             aEndpoint.getEndpointReference (),
                                                             aEndpoint.isRequireBusinessLevelSignature (),
                                                             aEndpoint.getMinimumAuthenticationLevel (),
                                                             aEndpoint.getServiceActivationDateTime (),
                                                             aEndpoint.getServiceExpirationDateTime (),
                                                             aEndpoint.getCertificate (),
                                                             aEndpoint.getServiceDescription (),
                                                             aEndpoint.getTechnicalContactUrl (),
                                                             aEndpoint.getTechnicalInformationUrl (),
                                                             aEndpoint.getExtensionsAsString ());
              aDBProcess.getEndpoints ().add (aDBEndpoint);
              aEM.persist (aDBEndpoint);
            }
          }

          aDBProcess.setServiceMetadata (aDBMetadata);
          aDBProcess.setExtension (aProcess.getExtensionsAsString ());
          break;
        }

      if (!bProcessFound)
      {
        // Not contained in new set
        aDBMetadata.getProcesses ().remove (aDBProcess);
        aEM.remove (aDBProcess);
      }
    }

    // Search for new processes
    for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
    {
      boolean bProcessFound = false;
      for (final DBProcess aDBProcess : aDBMetadata.getProcesses ())
        if (aDBProcess.getId ().getAsProcessIdentifier ().hasSameContent (aProcess.getProcessIdentifier ()))
        {
          bProcessFound = true;
          break;
        }

      if (!bProcessFound)
      {
        // Create a new process with new endpoints
        final DBProcess aDBProcess = new DBProcess (new DBProcessID (aDBMetadata.getId (),
                                                                     aProcess.getProcessIdentifier ()),
                                                    aDBMetadata,
                                                    aProcess.getExtensionsAsString ());
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final DBEndpoint aDBEndpoint = new DBEndpoint (new DBEndpointID (aDBProcess.getId (),
                                                                           aEndpoint.getTransportProfile ()),
                                                         aDBProcess,
                                                         aEndpoint.getEndpointReference (),
                                                         aEndpoint.isRequireBusinessLevelSignature (),
                                                         aEndpoint.getMinimumAuthenticationLevel (),
                                                         aEndpoint.getServiceActivationDateTime (),
                                                         aEndpoint.getServiceExpirationDateTime (),
                                                         aEndpoint.getCertificate (),
                                                         aEndpoint.getServiceDescription (),
                                                         aEndpoint.getTechnicalContactUrl (),
                                                         aEndpoint.getTechnicalInformationUrl (),
                                                         aEndpoint.getExtensionsAsString ());
          aDBProcess.getEndpoints ().add (aDBEndpoint);
          aEM.persist (aDBEndpoint);
        }
        aDBMetadata.getProcesses ().add (aDBProcess);
        aEM.persist (aDBProcess);
      }
    }

    aDBMetadata.setExtension (aServiceInfo.getExtensionsAsString ());
  }

  @Nonnull
  public ESuccess mergeSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    final MutableBoolean aUpdated = new MutableBoolean (false);
    JPAExecutionResult <DBServiceMetadata> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();
      final DBServiceMetadataID aDBMetadataID = new DBServiceMetadataID (aSMPServiceInformation.getServiceGroup ()
                                                                                               .getParticpantIdentifier (),
                                                                         aSMPServiceInformation.getDocumentTypeIdentifier ());
      DBServiceMetadata aDBMetadata = aEM.find (DBServiceMetadata.class, aDBMetadataID);
      aUpdated.set (aDBMetadata != null);
      if (aDBMetadata != null)
      {
        // Edit an existing one
        _update (aEM, aDBMetadata, aSMPServiceInformation);
        aEM.merge (aDBMetadata);
      }
      else
      {
        // Create a new one
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aSMPServiceInformation.getServiceGroup ()
                                                                                               .getParticpantIdentifier ());
        final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
        if (aDBServiceGroup == null)
          throw new IllegalStateException ("Failed to resolve service group for " + aSMPServiceInformation);

        aDBMetadata = new DBServiceMetadata (aDBMetadataID,
                                             aDBServiceGroup,
                                             aSMPServiceInformation.getExtensionsAsString ());
        _update (aEM, aDBMetadata, aSMPServiceInformation);
        aEM.persist (aDBMetadata);
      }
      return aDBMetadata;
    });

    if (ret.hasException ())
      return ESuccess.FAILURE;

    // Callback outside of transaction
    if (aUpdated.booleanValue ())
      m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
    else
      m_aCBs.forEach (x -> x.onSMPServiceInformationCreated (aSMPServiceInformation));

    return ESuccess.SUCCESS;
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
        if (aEndpoint != null)
          return aServiceInfo;
      }
    }
    return null;
  }

  @Nonnull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (aSMPServiceInformation == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();
      final DBServiceMetadataID aDBMetadataID = new DBServiceMetadataID (aSMPServiceInformation.getServiceGroup ()
                                                                                               .getParticpantIdentifier (),
                                                                         aSMPServiceInformation.getDocumentTypeIdentifier ());
      final DBServiceMetadata aDBMetadata = aEM.find (DBServiceMetadata.class, aDBMetadataID);
      if (aDBMetadata == null)
        return EChange.UNCHANGED;

      aEM.remove (aDBMetadata);
      return EChange.CHANGED;
    });

    if (ret.hasException ())
      return EChange.UNCHANGED;

    // Callback outside of transaction
    if (ret.get ().isChanged ())
      m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return ret.get ();
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    final Wrapper <ICommonsList <ISMPServiceInformation>> aDeletedInfos = new Wrapper <> ();
    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      // Remember what will be deleted (never null)
      aDeletedInfos.set (getAllSMPServiceInformationOfServiceGroup (aServiceGroup));

      final int nCnt = getEntityManager ().createQuery ("DELETE FROM DBServiceMetadata p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                        DBServiceMetadataRedirection.class)
                                          .setParameter ("scheme",
                                                         aServiceGroup.getParticpantIdentifier ().getScheme ())
                                          .setParameter ("value", aServiceGroup.getParticpantIdentifier ().getValue ())
                                          .executeUpdate ();

      if (aDeletedInfos.get ().size () != nCnt)
        if (LOGGER.isWarnEnabled ())
          LOGGER.warn ("Retrieved " +
                       aDeletedInfos.get ().size () +
                       " ServiceMetadata entries but deleted " +
                       nCnt +
                       " - this number should be identical!");

      return EChange.valueOf (nCnt > 0);
    });
    if (ret.hasException ())
      return EChange.UNCHANGED;

    // Callback outside of transaction
    for (final ISMPServiceInformation aSMPServiceInformation : aDeletedInfos.get ())
      m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return ret.get ();
  }

  @Nonnull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    if (aSMPServiceInformation == null || aProcess == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      final int nCnt = getEntityManager ().createQuery ("DELETE FROM DBProcess p WHERE" +
                                                        " p.id.businessIdentifierScheme = :bischeme AND p.id.businessIdentifier = :bivalue AND" +
                                                        " p.id.documentIdentifierScheme = :discheme AND p.id.documentIdentifier = :divalue AND" +
                                                        " p.id.processIdentifierScheme = :pischeme AND p.id.processIdentifier = :pivalue",
                                                        DBServiceMetadataRedirection.class)
                                          .setParameter ("bischeme",
                                                         aSMPServiceInformation.getServiceGroup ()
                                                                               .getParticpantIdentifier ()
                                                                               .getScheme ())
                                          .setParameter ("bivalue",
                                                         aSMPServiceInformation.getServiceGroup ()
                                                                               .getParticpantIdentifier ()
                                                                               .getValue ())
                                          .setParameter ("discheme",
                                                         aSMPServiceInformation.getDocumentTypeIdentifier ()
                                                                               .getScheme ())
                                          .setParameter ("divalue",
                                                         aSMPServiceInformation.getDocumentTypeIdentifier ()
                                                                               .getValue ())
                                          .setParameter ("pischeme", aProcess.getProcessIdentifier ().getScheme ())
                                          .setParameter ("pivalue", aProcess.getProcessIdentifier ().getValue ())
                                          .executeUpdate ();
      return EChange.valueOf (nCnt > 0);
    });
    if (ret.hasException ())
      return EChange.UNCHANGED;

    return ret.get ();
  }

  @Nonnull
  private SMPServiceInformation _convert (@Nonnull final DBServiceMetadata aDBMetadata)
  {
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
    for (final DBProcess aDBProcess : aDBMetadata.getProcesses ())
    {
      final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
      for (final DBEndpoint aDBEndpoint : aDBProcess.getEndpoints ())
      {
        final SMPEndpoint aEndpoint = new SMPEndpoint (aDBEndpoint.getId ().getTransportProfile (),
                                                       aDBEndpoint.getEndpointReference (),
                                                       aDBEndpoint.isRequireBusinessLevelSignature (),
                                                       aDBEndpoint.getMinimumAuthenticationLevel (),
                                                       aDBEndpoint.getServiceActivationDate (),
                                                       aDBEndpoint.getServiceExpirationDate (),
                                                       aDBEndpoint.getCertificate (),
                                                       aDBEndpoint.getServiceDescription (),
                                                       aDBEndpoint.getTechnicalContactUrl (),
                                                       aDBEndpoint.getTechnicalInformationUrl (),
                                                       aDBEndpoint.getExtension ());
        aEndpoints.add (aEndpoint);
      }
      final SMPProcess aProcess = new SMPProcess (aDBProcess.getId ().getAsProcessIdentifier (),
                                                  aEndpoints,
                                                  aDBProcess.getExtension ());
      aProcesses.add (aProcess);
    }
    return new SMPServiceInformation (m_aServiceGroupMgr.getSMPServiceGroupOfID (aDBMetadata.getId ()
                                                                                            .getAsBusinessIdentifier ()),
                                      aDBMetadata.getId ().getAsDocumentTypeIdentifier (),
                                      aProcesses,
                                      aDBMetadata.getExtension ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    JPAExecutionResult <List <DBServiceMetadata>> ret;
    ret = doInTransaction ( () -> getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadata p",
                                                                   DBServiceMetadata.class)
                                                     .getResultList ());
    if (ret.hasException ())
      return new CommonsArrayList <> ();

    final ICommonsList <ISMPServiceInformation> aServiceInformations = new CommonsArrayList <> ();
    for (final DBServiceMetadata aDBMetadata : ret.get ())
      aServiceInformations.add (_convert (aDBMetadata));
    return aServiceInformations;
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect ( () -> {
      final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceMetadata p"));
      return Long.valueOf (nCount);
    });
    if (ret.hasException ())
      return 0;

    return ret.get ().longValue ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <ISMPServiceInformation> aServiceInformations = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      JPAExecutionResult <List <DBServiceMetadata>> ret;
      ret = doInTransaction ( () -> getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadata p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                                     DBServiceMetadata.class)
                                                       .setParameter ("scheme",
                                                                      aServiceGroup.getParticpantIdentifier ()
                                                                                   .getScheme ())
                                                       .setParameter ("value",
                                                                      aServiceGroup.getParticpantIdentifier ()
                                                                                   .getValue ())
                                                       .getResultList ());
      if (!ret.hasException ())
      {
        for (final DBServiceMetadata aDBMetadata : ret.get ())
          aServiceInformations.add (_convert (aDBMetadata));
      }
    }
    return aServiceInformations;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      for (final ISMPServiceInformation aServiceInformation : getAllSMPServiceInformationOfServiceGroup (aServiceGroup))
        ret.add (aServiceInformation.getDocumentTypeIdentifier ());
    }
    return ret;
  }

  @Nullable
  private DBServiceMetadata _getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                    @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null || aDocTypeID == null)
      return null;

    JPAExecutionResult <DBServiceMetadata> ret;
    ret = doInTransaction ( () -> {
      // Disable caching here
      final ICommonsMap <String, Object> aProps = new CommonsHashMap <> ();
      aProps.put ("eclipselink.cache-usage", CacheUsage.DoNotCheckCache);
      final DBServiceMetadataID aDBMetadataID = new DBServiceMetadataID (aServiceGroup.getParticpantIdentifier (),
                                                                         aDocTypeID);
      return getEntityManager ().find (DBServiceMetadata.class, aDBMetadataID, aProps);
    });
    if (ret.hasException ())
      return null;

    return ret.get ();
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    final DBServiceMetadata aDBMetadata = _getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                  aDocTypeID);
    if (aDBMetadata == null)
      return null;
    return _convert (aDBMetadata);
  }
}
