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
package com.helger.phoss.smp.domain.servicegroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.bdxr.smp1.participant.BDXR1ParticipantIdentifier;
import com.helger.peppolid.bdxr.smp2.participant.BDXR2ParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;

/**
 * This class represents a single service group.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPServiceGroup extends AbstractSMPHasExtension implements ISMPServiceGroup
{
  public static final ObjectType OT = new ObjectType ("smpservicegroup");

  private final String m_sID;
  private String m_sOwnerID;

  // Status member
  private final IParticipantIdentifier m_aParticipantIdentifier;

  /**
   * Create a unified participant identifier with a lower cased value, because
   * according to the PEPPOL policy for identifiers, the values must be treated
   * case-sensitive.
   *
   * @param aParticipantIdentifier
   *        The original participant identifier. May not be <code>null</code>.
   * @return The new participant identifier with a lower cased value.
   */
  @Nonnull
  public static IParticipantIdentifier createUnifiedParticipantIdentifier (@Nonnull final IParticipantIdentifier aParticipantIdentifier)
  {
    ValueEnforcer.notNull (aParticipantIdentifier, "ParticipantIdentifier");
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier ret = aIdentifierFactory.getClone (aParticipantIdentifier);
    if (ret == null)
      throw new IllegalStateException ("Failed to clone " +
                                       aParticipantIdentifier +
                                       " with identifier factory " +
                                       aIdentifierFactory);
    return ret;
  }

  public SMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                          @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                          @Nullable final String sExtension)
  {
    m_sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
    setOwnerID (sOwnerID);
    setExtensionAsString (sExtension);
    // Make a copy to avoid unwanted changes
    m_aParticipantIdentifier = createUnifiedParticipantIdentifier (aParticipantIdentifier);
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getOwnerID ()
  {
    return m_sOwnerID;
  }

  @Nonnull
  public final EChange setOwnerID (@Nonnull @Nonempty final String sOwnerID)
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    if (sOwnerID.equals (m_sOwnerID))
      return EChange.UNCHANGED;
    m_sOwnerID = sOwnerID;
    return EChange.CHANGED;
  }

  @Nonnull
  public IParticipantIdentifier getParticpantIdentifier ()
  {
    return m_aParticipantIdentifier;
  }

  @Nonnull
  public com.helger.peppol.smp.ServiceGroupType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.ServiceGroupType ret = new com.helger.peppol.smp.ServiceGroupType ();
    // Explicit constructor call is needed here!
    ret.setParticipantIdentifier (new SimpleParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (getAsPeppolExtension ());
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp1.ServiceGroupType getAsJAXBObjectBDXR1 ()
  {
    final com.helger.xsds.bdxr.smp1.ServiceGroupType ret = new com.helger.xsds.bdxr.smp1.ServiceGroupType ();
    // Explicit constructor call is needed here!
    ret.setParticipantIdentifier (new BDXR1ParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (getAsBDXRExtension ());
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp2.ServiceGroupType getAsJAXBObjectBDXR2 ()
  {
    final com.helger.xsds.bdxr.smp2.ServiceGroupType ret = new com.helger.xsds.bdxr.smp2.ServiceGroupType ();
    ret.setSMPExtensions (getAsBDXR2Extension ());
    ret.setSMPVersionID ("2.0");
    // Explicit constructor call is needed here!
    ret.setParticipantID (new BDXR2ParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceReference (null);
    }
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPServiceGroup rhs = (SMPServiceGroup) o;
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("ID", m_sID)
                            .append ("OwnerID", m_sOwnerID)
                            .append ("ParticipantIdentifier", m_aParticipantIdentifier)
                            .getToString ();
  }

  @Nonnull
  @Nonempty
  public static String createSMPServiceGroupID (@Nonnull final IParticipantIdentifier aParticipantIdentifier)
  {
    return createUnifiedParticipantIdentifier (aParticipantIdentifier).getURIEncoded ();
  }
}
