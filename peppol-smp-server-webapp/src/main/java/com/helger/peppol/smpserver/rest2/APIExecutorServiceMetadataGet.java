package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerSignedServiceMetadataType;
import com.helger.peppol.smp.marshal.SMPMarshallerSignedServiceMetadataType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.SMPWebAppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.XMLTransformerFactory;

public final class APIExecutorServiceMetadataGet implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceMetadataGet.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      /*
       * PD integration is disabled
       */
      LOGGER.warn ("The " +
                   SMPWebAppConfiguration.getDirectoryName () +
                   " integration is disabled. getBusinessCard will not be executed.");
      aUnifiedResponse.createNotFound ();
    }
    else
    {
      final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
      final String sDocumentTypeID = aPathVariables.get (Rest2Filter.PARAM_DOCUMENT_TYPE_ID);
      final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);

      // Create the unsigned response document
      Document aDoc;
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          final com.helger.peppol.smp.SignedServiceMetadataType ret = new SMPServerAPI (aDataProvider).getServiceRegistration (sServiceGroupID,
                                                                                                                               sDocumentTypeID);

          // Convert to DOM document
          final SMPMarshallerSignedServiceMetadataType aMarshaller = new SMPMarshallerSignedServiceMetadataType ();
          aDoc = aMarshaller.getAsDocument (ret);
          break;
        }
        case BDXR:
        {
          final com.helger.peppol.bdxr.SignedServiceMetadataType ret = new BDXRServerAPI (aDataProvider).getServiceRegistration (sServiceGroupID,
                                                                                                                                 sDocumentTypeID);

          // Convert to DOM document
          final BDXRMarshallerSignedServiceMetadataType aMarshaller = new BDXRMarshallerSignedServiceMetadataType ();
          aDoc = aMarshaller.getAsDocument (ret);
          break;
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }

      // Sign the document
      try
      {
        SMPKeyManager.getInstance ()
                     .signXML (aDoc.getDocumentElement (), SMPServerConfiguration.getRESTType ().isBDXR ());
      }
      catch (final Exception ex)
      {
        throw new RuntimeException ("Error in signing xml", ex);
      }

      // Serialize the signed document
      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        if (false)
        {
          // IMPORTANT: no indent and no align!
          final IXMLWriterSettings aSettings = XMLWriterSettings.createForCanonicalization ();

          // Write the result to a byte array
          if (XMLWriter.writeToStream (aDoc, aBAOS, aSettings).isFailure ())
            throw new IllegalStateException ("Failed to serialize signed node!");
        }
        else
        {
          // Use this because it correctly serializes &#13; which is important
          // for validating the signature!
          try
          {
            XMLTransformerFactory.newTransformer ().transform (new DOMSource (aDoc), new StreamResult (aBAOS));
          }
          catch (final TransformerException ex)
          {
            throw new IllegalStateException ("Failed to serialized signed node", ex);
          }
        }

        aUnifiedResponse.setContent (aBAOS.toByteArray ()).setMimeType (CMimeType.TEXT_XML);
      }
    }
  }
}
