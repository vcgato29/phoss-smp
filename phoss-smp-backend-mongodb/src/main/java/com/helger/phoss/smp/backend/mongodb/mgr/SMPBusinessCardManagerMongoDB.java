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
package com.helger.phoss.smp.backend.mongodb.mgr;

import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardContact;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

/**
 * Manager for all {@link SMPBusinessCard} objects.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardManagerMongoDB extends AbstractManagerMongoDB implements ISMPBusinessCardManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBusinessCardManagerMongoDB.class);

  private static final String BSON_ID = "id";
  private static final String BSON_SERVICE_GROUP_ID = "sgid";
  private static final String BSON_ENTITIES = "entities";
  private static final String BSON_NAMES = "names";
  private static final String BSON_COUNTRYCODE = "countrycode";
  private static final String BSON_GEOINFO = "geoinfo";
  private static final String BSON_IDS = "ids";
  private static final String BSON_WEBSITES = "websites";
  private static final String BSON_CONTACTS = "contacts";
  private static final String BSON_ADDITIONAL = "additional";
  private static final String BSON_REGDATE = "regdate";
  private static final String BSON_TYPE = "type";
  private static final String BSON_NAME = "name";
  private static final String BSON_PHONE = "phone";
  private static final String BSON_EMAIL = "email";
  private static final String BSON_SCHEME = "scheme";
  private static final String BSON_VALUE = "value";
  private static final String BSON_LANGUAGE = "language";

  private final IIdentifierFactory m_aIdentifierFactory;
  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  public SMPBusinessCardManagerMongoDB (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                        @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    super ("smp-businesscard");
    m_aIdentifierFactory = aIdentifierFactory;
    m_aServiceGroupMgr = aServiceGroupMgr;
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPBusinessCardCallback> bcCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final SMPBusinessCardName aValue)
  {
    final Document ret = new Document ().append (BSON_NAME, aValue.getName ());
    if (aValue.hasLanguageCode ())
      ret.append (BSON_LANGUAGE, aValue.getLanguageCode ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPBusinessCardName toBCName (@Nonnull final Document aDoc)
  {
    return new SMPBusinessCardName (aDoc.getString (BSON_NAME), aDoc.getString (BSON_LANGUAGE));
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final SMPBusinessCardIdentifier aValue)
  {
    return new Document ().append (BSON_ID, aValue.getID ())
                          .append (BSON_SCHEME, aValue.getScheme ())
                          .append (BSON_VALUE, aValue.getValue ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPBusinessCardIdentifier toBCIdentifier (@Nonnull final Document aDoc)
  {
    return new SMPBusinessCardIdentifier (aDoc.getString (BSON_ID),
                                          aDoc.getString (BSON_SCHEME),
                                          aDoc.getString (BSON_VALUE));
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final SMPBusinessCardContact aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ());
    if (aValue.hasType ())
      ret.append (BSON_TYPE, aValue.getType ());
    if (aValue.hasName ())
      ret.append (BSON_NAME, aValue.getName ());
    if (aValue.hasPhoneNumber ())
      ret.append (BSON_PHONE, aValue.getPhoneNumber ());
    if (aValue.hasEmail ())
      ret.append (BSON_EMAIL, aValue.getEmail ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPBusinessCardContact toBCContact (@Nonnull final Document aDoc)
  {
    return new SMPBusinessCardContact (aDoc.getString (BSON_ID),
                                       aDoc.getString (BSON_TYPE),
                                       aDoc.getString (BSON_NAME),
                                       aDoc.getString (BSON_PHONE),
                                       aDoc.getString (BSON_EMAIL));
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final SMPBusinessCardEntity aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ());
    // Mandatory fields
    {
      final ICommonsList <Document> aNames = new CommonsArrayList <> ();
      for (final SMPBusinessCardName aName : aValue.names ())
        aNames.add (toBson (aName));
      ret.append (BSON_NAMES, aNames);
    }
    ret.append (BSON_COUNTRYCODE, aValue.getCountryCode ());

    // Optional fields
    if (aValue.hasGeographicalInformation ())
      ret.append (BSON_GEOINFO, aValue.getGeographicalInformation ());
    {
      final ICommonsList <Document> aIDs = new CommonsArrayList <> ();
      for (final SMPBusinessCardIdentifier aID : aValue.identifiers ())
        aIDs.add (toBson (aID));
      if (aIDs.isNotEmpty ())
        ret.append (BSON_IDS, aIDs);
    }
    if (aValue.websiteURIs ().isNotEmpty ())
      ret.append (BSON_WEBSITES, aValue.websiteURIs ());
    {
      final ICommonsList <Document> aContacts = new CommonsArrayList <> ();
      for (final SMPBusinessCardContact aContact : aValue.contacts ())
        aContacts.add (toBson (aContact));
      if (aContacts.isNotEmpty ())
        ret.append (BSON_CONTACTS, aContacts);
    }
    if (aValue.hasAdditionalInformation ())
      ret.append (BSON_ADDITIONAL, aValue.getAdditionalInformation ());
    if (aValue.hasRegistrationDate ())
      ret.append (BSON_REGDATE, aValue.getRegistrationDate ());

    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPBusinessCardEntity toBCEntity (@Nonnull final Document aDoc)
  {
    final SMPBusinessCardEntity ret = new SMPBusinessCardEntity (aDoc.getString (BSON_ID));
    for (final Document aItemDoc : aDoc.getList (BSON_NAMES, Document.class))
      ret.names ().add (toBCName (aItemDoc));
    ret.setCountryCode (aDoc.getString (BSON_COUNTRYCODE));
    ret.setGeographicalInformation (aDoc.getString (BSON_GEOINFO));
    for (final Document aItemDoc : aDoc.getList (BSON_IDS, Document.class))
      ret.identifiers ().add (toBCIdentifier (aItemDoc));
    for (final String sItem : aDoc.getList (BSON_WEBSITES, String.class))
      ret.websiteURIs ().add (sItem);
    for (final Document aItemDoc : aDoc.getList (BSON_CONTACTS, Document.class))
      ret.contacts ().add (toBCContact (aItemDoc));
    ret.setAdditionalInformation (aDoc.getString (BSON_ADDITIONAL));
    ret.setRegistrationDate (aDoc.get (BSON_REGDATE, LocalDate.class));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPBusinessCard aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ())
                                        .append (BSON_SERVICE_GROUP_ID, aValue.getServiceGroupID ());
    final ICommonsList <Document> aEntities = new CommonsArrayList <> ();
    for (final SMPBusinessCardEntity aEntity : aValue.getAllEntities ())
      aEntities.add (toBson (aEntity));
    if (aEntities.isNotEmpty ())
      ret.append (BSON_ENTITIES, aEntities);
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public SMPBusinessCard toDomain (@Nonnull final Document aDoc)
  {
    final ISMPServiceGroup aServiceGroup = m_aServiceGroupMgr.getSMPServiceGroupOfID (m_aIdentifierFactory.parseParticipantIdentifier (aDoc.getString (BSON_SERVICE_GROUP_ID)));
    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
    for (final Document aItemDoc : aDoc.getList (BSON_ENTITIES, Document.class))
      aEntities.add (toBCEntity (aItemDoc));
    // The ID itself is derived from ServiceGroupID
    return new SMPBusinessCard (aServiceGroup, aEntities);
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _createSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard)
  {
    getCollection ().insertOne (toBson (aSMPBusinessCard));

    AuditHelper.onAuditCreateSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      aSMPBusinessCard.getServiceGroupID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _updateSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard)
  {
    final Document aOldDoc = getCollection ().findOneAndReplace (new Document (BSON_ID, aSMPBusinessCard.getID ()),
                                                                 toBson (aSMPBusinessCard));
    if (aOldDoc != null)
      AuditHelper.onAuditModifySuccess (SMPBusinessCard.OT,
                                        aSMPBusinessCard.getID (),
                                        aSMPBusinessCard.getServiceGroupID (),
                                        Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  /**
   * Create or update a business card for a service group.
   *
   * @param aServiceGroup
   *        Service group
   * @param aEntities
   *        The entities of the business card. May not be <code>null</code>.
   * @return The new or updated {@link ISMPBusinessCard}. Never
   *         <code>null</code>.
   */
  @Nonnull
  public ISMPBusinessCard createOrUpdateSMPBusinessCard (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                         @Nonnull final Collection <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aEntities, "Entities");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPBusinessCard (" +
                    aServiceGroup.getParticpantIdentifier ().getURIEncoded () +
                    ", " +
                    aEntities.size () +
                    " entities)");

    final ISMPBusinessCard aOldBusinessCard = getSMPBusinessCardOfServiceGroup (aServiceGroup);
    final SMPBusinessCard aNewBusinessCard = new SMPBusinessCard (aServiceGroup, aEntities);
    if (aOldBusinessCard != null)
    {
      // Reuse old ID
      _updateSMPBusinessCard (aNewBusinessCard);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPBusinessCard update successful");
    }
    else
    {
      // Create new ID
      _createSMPBusinessCard (aNewBusinessCard);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPBusinessCard create successful");
    }

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onCreateOrUpdateSMPBusinessCard (aNewBusinessCard));

    return aNewBusinessCard;
  }

  @Nonnull
  public EChange deleteSMPBusinessCard (@Nullable final ISMPBusinessCard aSMPBusinessCard)
  {
    if (aSMPBusinessCard == null)
      return EChange.UNCHANGED;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard (" + aSMPBusinessCard.getID () + ")");

    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, aSMPBusinessCard.getID ()));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPBusinessCard.OT, "no-such-id", aSMPBusinessCard.getID ());
      return EChange.UNCHANGED;
    }

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onDeleteSMPBusinessCard (aSMPBusinessCard));

    AuditHelper.onAuditDeleteSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      aSMPBusinessCard.getServiceGroupID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard successful");

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    final ICommonsList <ISMPBusinessCard> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach ((Consumer <Document>) x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {

    if (aServiceGroup == null)
      return null;

    return getSMPBusinessCardOfID (aServiceGroup.getID ());
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    return getCollection ().find (new Document (BSON_ID, sID)).map (x -> toDomain (x)).first ();
  }

  @Nonnegative
  public long getSMPBusinessCardCount ()
  {
    return getCollection ().countDocuments ();
  }
}
