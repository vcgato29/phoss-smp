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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ProcessListType;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smp.RedirectType;
import com.helger.peppol.smp.ServiceEndpointList;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smp.ServiceInformationType;
import com.helger.peppol.smp.ServiceMetadataReferenceCollectionType;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smpclient.SMPClient;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.smpclient.exception.SMPClientNotFoundException;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PeppolProcessIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.mock.MockSMPClient;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.phoss.smp.rest2.Rest2Filter;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test class for class {@link Rest2Filter}.
 *
 * @author Philip Helger
 */
public final class ServiceMetadataInterfaceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceMetadataInterfaceTest.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials ("peppol_user",
                                                                                                "Test1234");

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (ClassPathResource.getAsFile ("test-smp-server-sql.properties")
                                                                                           .getAbsolutePath ());

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  @Nonnull
  private static Builder _addCredentials (@Nonnull final Builder aBuilder)
  {
    // Use default credentials for SQL backend
    return aBuilder.header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ());
  }

  private static int _testResponseJerseyClient (final Response aResponseMsg, final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    assertNotNull (aResponseMsg);
    // Read response
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      LOGGER.info ("HTTP Response: " + sResponse);
    assertTrue (Arrays.toString (aStatusCodes) + " does not contain " + aResponseMsg.getStatus (),
                ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()));
    return aResponseMsg.getStatus ();
  }

  @Test
  public void testCreateAndDeleteServiceInformationJerseyClient ()
  {
    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      // Lower case
      final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
      final String sPI_LC = aPI_LC.getURIEncoded ();
      // Upper case
      final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
      final String sPI_UC = aPI_UC.getURIEncoded ();

      final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
      final String sDT = aDT.getURIEncoded ();

      final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS4A_V2.getAsProcessIdentifier ();

      final ServiceGroupType aSG = new ServiceGroupType ();
      aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
      aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

      final ServiceMetadataType aSM = new ServiceMetadataType ();
      final ServiceInformationType aSI = new ServiceInformationType ();
      aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
      aSI.setDocumentIdentifier (aDT);
      {
        final ProcessListType aPL = new ProcessListType ();
        final ProcessType aProcess = new ProcessType ();
        aProcess.setProcessIdentifier (aProcID);
        final ServiceEndpointList aSEL = new ServiceEndpointList ();
        final EndpointType aEndpoint = new EndpointType ();
        aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as2"));
        aEndpoint.setRequireBusinessLevelSignature (false);
        aEndpoint.setCertificate ("blacert");
        aEndpoint.setServiceDescription ("Unit test service");
        aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
        aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_AS2.getID ());
        aSEL.addEndpoint (aEndpoint);
        aProcess.setServiceEndpointList (aSEL);
        aPL.addProcess (aProcess);
        aSI.setProcessList (aPL);
      }
      aSM.setServiceInformation (aSI);

      final WebTarget aTarget = ClientBuilder.newClient ().target (m_aRule.getFullURL ());
      Response aResponseMsg;

      final int nStatus = _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), new int [] { 404, 500 });
      if (nStatus == 500)
      {
        // Seems like MySQL is not running
        return;
      }
      _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);

      try
      {
        // PUT ServiceGroup
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
        _testResponseJerseyClient (aResponseMsg, 200);

        // Read both
        assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
        assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));

        final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_LC);
        assertNotNull (aServiceGroup);
        final ISMPServiceGroup aServiceGroup_UC = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_UC);
        assertEquals (aServiceGroup, aServiceGroup_UC);

        try
        {
          // PUT 1 ServiceInformation
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                                 .path ("services")
                                                 .path (sDT)
                                                 .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
          _testResponseJerseyClient (aResponseMsg, 200);
          assertNotNull (SMPMetaManager.getServiceInformationMgr ()
                                       .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // PUT 2 ServiceInformation
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                                 .path ("services")
                                                 .path (sDT)
                                                 .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
          _testResponseJerseyClient (aResponseMsg, 200);
          assertNotNull (SMPMetaManager.getServiceInformationMgr ()
                                       .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // DELETE 1 ServiceInformation
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
          _testResponseJerseyClient (aResponseMsg, 200);
          assertNull (SMPMetaManager.getServiceInformationMgr ()
                                    .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }
        finally
        {
          // DELETE 2 ServiceInformation
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
          _testResponseJerseyClient (aResponseMsg, 200, 404);
          assertNull (SMPMetaManager.getServiceInformationMgr ()
                                    .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }

        assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      }
      finally
      {
        // DELETE ServiceGroup
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
        // May be 500 if no MySQL is running
        _testResponseJerseyClient (aResponseMsg, 200, 404);

        _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), 404);
        _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);
        assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
        assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));
      }
    }
  }

  @Test
  public void testCreateAndDeleteServiceInformationSMPClient () throws SMPClientException
  {
    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      // Lower case
      final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
      // Upper case
      final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");

      final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();

      final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS4A_V2.getAsProcessIdentifier ();

      final ServiceGroupType aSG = new ServiceGroupType ();
      aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
      aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

      final ServiceInformationType aSI = new ServiceInformationType ();
      aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
      aSI.setDocumentIdentifier (aDT);
      {
        final ProcessListType aPL = new ProcessListType ();
        final ProcessType aProcess = new ProcessType ();
        aProcess.setProcessIdentifier (aProcID);
        final ServiceEndpointList aSEL = new ServiceEndpointList ();
        final EndpointType aEndpoint = new EndpointType ();
        aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as2"));
        aEndpoint.setRequireBusinessLevelSignature (false);
        aEndpoint.setCertificate ("blacert");
        aEndpoint.setServiceDescription ("Unit test service");
        aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
        aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_AS2.getID ());
        aSEL.addEndpoint (aEndpoint);
        aProcess.setServiceEndpointList (aSEL);
        aPL.addProcess (aProcess);
        aSI.setProcessList (aPL);
      }

      final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceInformationManager aSIMgr = SMPMetaManager.getServiceInformationMgr ();
      final SMPClient aSMPClient = new MockSMPClient ();

      try
      {
        assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      }
      catch (final SMPClientException ex)
      {
        // Seems like MySQL is not running
        return;
      }
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      try
      {
        // PUT ServiceGroup
        aSMPClient.saveServiceGroup (aSG, CREDENTIALS);

        // Read both
        assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
        assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
        assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
        assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

        final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
        assertNotNull (aServiceGroup);
        final ISMPServiceGroup aServiceGroup_UC = aSGMgr.getSMPServiceGroupOfID (aPI_UC);
        assertEquals (aServiceGroup, aServiceGroup_UC);

        try
        {
          // PUT 1 ServiceInformation
          aSMPClient.saveServiceInformation (aSI, CREDENTIALS);
          assertNotNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // PUT 2 ServiceInformation
          aSMPClient.saveServiceInformation (aSI, CREDENTIALS);
          assertNotNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // DELETE 1 ServiceInformation
          aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
          assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }
        finally
        {
          // DELETE 2 ServiceInformation
          try
          {
            aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
          }
          catch (final SMPClientNotFoundException ex)
          {
            // Expected
          }
          assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }

        assertNotNull (aSMPClient.getServiceGroup (aPI_LC));
      }
      finally
      {
        // DELETE ServiceGroup
        try
        {
          aSMPClient.deleteServiceGroup (aPI_LC, CREDENTIALS);
        }
        catch (final SMPClientNotFoundException ex)
        {
          // Expected
        }

        assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
        assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
        assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
        assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
      }
    }
  }

  @Test
  public void testCreateAndDeleteRedirectJerseyClient ()
  {
    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      // Lower case
      final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
      final String sPI_LC = aPI_LC.getURIEncoded ();
      // Upper case
      final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
      final String sPI_UC = aPI_UC.getURIEncoded ();

      final IDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
      final String sDT = aDT.getURIEncoded ();

      final ServiceGroupType aSG = new ServiceGroupType ();
      aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
      aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

      final ServiceMetadataType aSM = new ServiceMetadataType ();
      final RedirectType aRedir = new RedirectType ();
      aRedir.setHref ("http://other-smp.domain.xyz");
      aRedir.setCertificateUID ("APP_0000000000000");
      aSM.setRedirect (aRedir);

      final WebTarget aTarget = ClientBuilder.newClient ().target (m_aRule.getFullURL ());
      Response aResponseMsg;

      final int nStatus = _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), new int [] { 404, 500 });
      if (nStatus == 500)
      {
        // Seems like MySQL is not running
        return;
      }
      _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);

      try
      {
        // PUT ServiceGroup
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
        _testResponseJerseyClient (aResponseMsg, 200);

        assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
        assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));

        final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_LC);
        assertNotNull (aServiceGroup);
        final ISMPServiceGroup aServiceGroup_UC = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_UC);
        assertEquals (aServiceGroup, aServiceGroup_UC);

        try
        {
          // PUT 1 ServiceInformation
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                                 .path ("services")
                                                 .path (sDT)
                                                 .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
          _testResponseJerseyClient (aResponseMsg, 200);
          assertNotNull (SMPMetaManager.getRedirectMgr ()
                                       .getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // PUT 2 ServiceInformation
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                                 .path ("services")
                                                 .path (sDT)
                                                 .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
          _testResponseJerseyClient (aResponseMsg, 200);
          assertNotNull (SMPMetaManager.getRedirectMgr ()
                                       .getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // DELETE 1 Redirect
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
          _testResponseJerseyClient (aResponseMsg, 200);
          assertNull (SMPMetaManager.getRedirectMgr ()
                                    .getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }
        finally
        {
          // DELETE 2 Redirect
          aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
          _testResponseJerseyClient (aResponseMsg, 200, 404);
          assertNull (SMPMetaManager.getRedirectMgr ()
                                    .getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }

        assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      }
      finally
      {
        // DELETE ServiceGroup
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
        _testResponseJerseyClient (aResponseMsg, 200, 404);

        _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), 404);
        _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);
        assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
        assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));
      }
    }
  }

  @Test
  public void testCreateAndDeleteRedirectSMPClient () throws SMPClientException
  {
    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      // Lower case
      final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
      // Upper case
      final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
      final IDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();

      final ServiceGroupType aSG = new ServiceGroupType ();
      aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
      aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

      final RedirectType aRedir = new RedirectType ();
      aRedir.setHref ("http://other-smp.domain.xyz");
      aRedir.setCertificateUID ("APP_0000000000000");

      final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPRedirectManager aSRMgr = SMPMetaManager.getRedirectMgr ();
      final SMPClient aSMPClient = new MockSMPClient ();

      try
      {
        assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      }
      catch (final SMPClientException ex)
      {
        // Seems like MySQL is not running
        return;
      }
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      try
      {
        // PUT ServiceGroup
        aSMPClient.saveServiceGroup (aSG, CREDENTIALS);

        assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
        assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
        assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
        assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

        final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
        assertNotNull (aServiceGroup);
        final ISMPServiceGroup aServiceGroup_UC = aSGMgr.getSMPServiceGroupOfID (aPI_UC);
        assertEquals (aServiceGroup, aServiceGroup_UC);

        try
        {
          // PUT 1 ServiceInformation
          aSMPClient.saveServiceRedirect (aPI_LC, aDT, aRedir, CREDENTIALS);
          assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // PUT 2 ServiceInformation
          aSMPClient.saveServiceRedirect (aPI_LC, aDT, aRedir, CREDENTIALS);
          assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));

          // DELETE 1 Redirect
          aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
          assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }
        finally
        {
          // DELETE 2 Redirect
          try
          {
            aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
          }
          catch (final SMPClientNotFoundException ex)
          {
            // Expected
          }
          assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
        }

        assertNotNull (aSGMgr.getSMPServiceGroupOfID (aPI_LC));
      }
      finally
      {
        // DELETE ServiceGroup
        try
        {
          aSMPClient.deleteServiceGroup (aPI_LC, CREDENTIALS);
        }
        catch (final SMPClientNotFoundException ex)
        {
          // Expected
        }

        assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
        assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
        assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
        assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
      }
    }
  }
}
