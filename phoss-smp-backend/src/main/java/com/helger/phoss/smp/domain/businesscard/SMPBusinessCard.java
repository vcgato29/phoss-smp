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
package com.helger.phoss.smp.domain.businesscard;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.pd.businesscard.v3.PD3APIHelper;
import com.helger.pd.businesscard.v3.PD3BusinessCardType;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;

/**
 * A single business card.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPBusinessCard implements ISMPBusinessCard
{
  public static final ObjectType OT = new ObjectType ("smpbusinesscard");

  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private final ICommonsList <SMPBusinessCardEntity> m_aEntities;

  public SMPBusinessCard (@Nonnull final ISMPServiceGroup aServiceGroup,
                          @Nonnull final Iterable <? extends SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aEntities, "Entities");

    m_sID = aServiceGroup.getID ();
    m_aServiceGroup = aServiceGroup;
    m_aEntities = new CommonsArrayList <> (aEntities);
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public ISMPServiceGroup getServiceGroup ()
  {
    return m_aServiceGroup;
  }

  /**
   * @return A mutable list with all {@link SMPBusinessCardEntity} objects.
   *         Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <SMPBusinessCardEntity> entities ()
  {
    return m_aEntities;
  }

  /**
   * @return A mutable list with all {@link SMPBusinessCardEntity} objects.
   *         Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <SMPBusinessCardEntity> getAllEntities ()
  {
    return m_aEntities.getClone ();
  }

  @Nonnegative
  public int getEntityCount ()
  {
    return m_aEntities.size ();
  }

  @Nonnull
  public PD3BusinessCardType getAsJAXBObject ()
  {
    final PD3BusinessCardType ret = new PD3BusinessCardType ();
    ret.setParticipantIdentifier (PD3APIHelper.createIdentifier (m_aServiceGroup.getParticpantIdentifier ()
                                                                                .getScheme (),
                                                                 m_aServiceGroup.getParticpantIdentifier ()
                                                                                .getValue ()));
    for (final SMPBusinessCardEntity aItem : m_aEntities)
      ret.addBusinessEntity (aItem.getAsJAXBObject ());
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCard rhs = (SMPBusinessCard) o;
    return m_aServiceGroup.equals (rhs.m_aServiceGroup) && m_aEntities.equals (rhs.m_aEntities);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aServiceGroup).append (m_aEntities).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("serviceGroup", m_aServiceGroup)
                                       .append ("entities", m_aEntities)
                                       .getToString ();
  }
}
