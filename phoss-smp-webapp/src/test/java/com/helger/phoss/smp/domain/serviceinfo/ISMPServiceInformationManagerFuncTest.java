/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.domain.serviceinfo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import javax.persistence.PersistenceException;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.Rule;
import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;

/**
 * Test class for class {@link ISMPServiceInformationManager}.
 *
 * @author Philip Helger
 */
public final class ISMPServiceInformationManagerFuncTest
{
  @Rule
  public final SMPServerTestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testAll () throws SMPServerException
  {
    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final IParticipantIdentifier aPI1 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:junittest1");
    final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::testdoc#ext:1.0");
    final IProcessIdentifier aProcessID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("junit-proc");

    final String sUserID = "junitserviceinfo";
    try
    {
      // May fail
      aUserMgr.createUser (sUserID, "bla");
    }
    catch (final PersistenceException ex)
    {
      assertTrue (ex.getCause () instanceof DatabaseException);
      // MySQL is not configured correctly!
      return;
    }

    try
    {
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1);
      final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (sUserID, aPI1, null);
      assertNotNull (aSG);
      try
      {
        final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
        final LocalDateTime aEndDT = aStartDT.plusYears (1);
        final SMPEndpoint aEP = new SMPEndpoint ("tp",
                                                 "http://localhost/as2",
                                                 false,
                                                 "minauth",
                                                 aStartDT,
                                                 aEndDT,
                                                 "cert",
                                                 "sd",
                                                 "tc",
                                                 "ti",
                                                 "<extep />");

        final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), "<extproc/>");

        final SMPServiceInformation aServiceInformation = new SMPServiceInformation (aSG,
                                                                                     aDocTypeID,
                                                                                     new CommonsArrayList <> (aProcess),
                                                                                     "<extsi/>");
        assertTrue (aServiceInfoMgr.mergeSMPServiceInformation (aServiceInformation).isSuccess ());
      }
      finally
      {
        // Don't care about the result
        aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1);
      }
    }
    finally
    {
      // Don't care about the result
      aUserMgr.deleteUser (sUserID);
    }
  }
}
