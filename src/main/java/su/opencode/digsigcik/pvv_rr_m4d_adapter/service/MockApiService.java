package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dConfirmDto;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dFnsDataResponse;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dRevokeConfirmDto;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockApiService {

    public List<String> createAndSignedM4dConfirm(M4dFnsDataResponse report) throws JAXBException, IOException {
        List<String> tempFileNames = new ArrayList<>();
        FileOutputStream fo;
        String startDate = report.getStartDateTime().substring(0, 10);
        String endDate = report.getEndDateTime().substring(0, 10);
        M4dConfirmDto m4dConfirmDto = getXmlM4dConfirm(report.getGuid(), "б/н", startDate, endDate, report.getCreatedDateTime());
        String xmlConfirmString = marshall(m4dConfirmDto, M4dConfirmDto.class);

        String filenameXml = "/tmp/" + m4dConfirmDto.getIdFile()+".xml";
        tempFileNames.add(filenameXml);
        fo = new FileOutputStream(filenameXml);
        byte[] bytes = xmlConfirmString.getBytes();
        fo.write(bytes);
        fo.close();
        //автоподписание
        String resource = "MIIFPQYJKoZIhvcNAQcCoIIFLjCCBSoCAQExDDAKBggqhQMHAQECAjALBgkqhkiG9w0BBwGgggNCMIIDPjCCAuugAwIBAgIQKz6EvSYQZbBD9TiKAZa6lDAKBggqhQMHAQEDAjA9MQswCQYDVQQGEwJSVTENMAsGA1UEChMEVEVTVDEfMB0GA1UEAx4WAEsARQBZAF8AVABFAFMAVABfAEMAQTAeFw0yNTA1MTAxNDEzMThaFw0yNjA4MTAxNDIzMThaMDUxCzAJBgNVBAYTAlJVMRUwEwYDVQQHDAzQnNC+0YHQutCy0LAxDzANBgNVBAMMBtCf0K3QnzBmMB8GCCqFAwcBAQEBMBMGByqFAwICJAAGCCqFAwcBAQICA0MABEBo8EHSkqqPJk1rstZkHgDz4/G5BI9OfD6Gf3R0VZU+a6Asn4W5mbPYcUS2jGxS8qIwSA1PRKHTY1AaUvtmsk3jo4IBxjCCAcIwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMEMF0GCCsGAQUFBwEBBFEwTzAnBggrBgEFBQcwAYYbaHR0cDovL2tleWxhYi9vY3NwL29jc3Auc3JmMCQGCCsGAQUFBzAChhhodHRwOi8va2V5bGFiL2FpYS9jYS5jcnQwDgYDVR0PAQH/BAQDAgPoMB8GCSsGAQQBgjcVBwQSMBAGCCqFAwICLgAIAgEBAgEAME8GA1UdHwRIMEYwRKBCoECGPmh0dHA6Ly9rZXlsYWIvY2RwL0UwNkI4OUUzRTMzRThFMDdGOTkxN0JERTIyNEU1RjREN0EwMkIyRkEuY3JsMHQGA1UdIwRtMGuAFOBriePjPo4H+ZF73iJOX016ArL6oUGkPzA9MQswCQYDVQQGEwJSVTENMAsGA1UEChMEVEVTVDEfMB0GA1UEAx4WAEsARQBZAF8AVABFAFMAVABfAEMAQYIQSdx6n79N2axN4uDdPxDPszAdBgNVHQ4EFgQUz20bHaT5wG76pSZAtjY6iw9WUo4wKwYDVR0QBCQwIoAPMjAyNTA1MTAxNDEzMThagQ8yMDI2MDgxMDE0MjMxOFowCgYIKoUDBwEBAwIDQQCyjpDbWResjln95Pil3RkRdvqi+m8zc9oBXiTy9wmKWu+PmqyxW8NuWUU71Qae81ziSpIXbE57MPvK0XSpfPM9MYIBwjCCAb4CAQEwUTA9MQswCQYDVQQGEwJSVTENMAsGA1UEChMEVEVTVDEfMB0GA1UEAx4WAEsARQBZAF8AVABFAFMAVABfAEMAQQIQKz6EvSYQZbBD9TiKAZa6lDAKBggqhQMHAQECAqCCAQowGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0BCQUxDxcNMjUwNTIyMDY1MjQ3WjAvBgkqhkiG9w0BCQQxIgQg3WXvbGBoOsUAtkGf/EsPJZD+uo/nzRb7aflyOVPwA0EwgZ4GCyqGSIb3DQEJEAIvMYGOMIGLMIGIMIGFMAoGCCqFAwcBAQICBCBBR180D+rIVRqeEGdYwDwx8LBl0XFsoms5Z596OlrY5jBVMEGkPzA9MQswCQYDVQQGEwJSVTENMAsGA1UEChMEVEVTVDEfMB0GA1UEAx4WAEsARQBZAF8AVABFAFMAVABfAEMAQQIQKz6EvSYQZbBD9TiKAZa6lDAKBggqhQMHAQEBAQRAZxjvdqv256TPw65vxl8F9Po2MJ/PApu7o+Lf610QIIgFQvkXwOozSSqRfQIAStRNZzeLPSZJa9HvR6Ik4+w5Mw==";

        String filenameSig = "/tmp/" + m4dConfirmDto.getIdFile()+"_detached.sig";
        tempFileNames.add(filenameSig);
        fo = new FileOutputStream(filenameSig);
        byte[] bytes1 = resource.getBytes();
        fo.write(bytes1);
        fo.close();
        return tempFileNames;
    }

    public List<String> createAndSignedM4dRevokeConfirm(M4dFnsDataResponse report) throws JAXBException, IOException {
        List<String> tempFileNames = new ArrayList<>();
        FileOutputStream fo;
        M4dRevokeConfirmDto m4dRevokeConfirmDto = getXmlM4dRevokeConfirm(report.getGuid(), "6oyh1wi6NrRqXjrkZ4nbocNEBgoM65mWXXh8RieZYNK6", report.getCreatedDateTime());
        String xmlConfirmString = marshall(m4dRevokeConfirmDto, M4dRevokeConfirmDto.class);

        String filenameXml = "/tmp/" + m4dRevokeConfirmDto.getIdFile()+".xml";
        tempFileNames.add(filenameXml);
        fo = new FileOutputStream(filenameXml);
        byte[] bytes = xmlConfirmString.getBytes();
        fo.write(bytes);
        fo.close();
        //автоподписание
        String resource = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/PjzQn9C+0LTRgtCy0LXRgNC20LTQtdC90LjQtSDQktC10YDRgdCk0L7RgNC8PSLQn9C+0LTRgtCy0KTQndChMDAzIiDQotC40L/Qn9C+0LTRgtCyPSLQn9C+0LTRgtCy0JfQsNGP0LLQu9Ce0YLQvNC10L3QsCIg0JjQtNCk0LDQudC7PSLQmtCy0LjRgjAwM1/QpNCd0KFf0JfQsNGP0LLQntGC0LxfZWVjYmZmZWMtZTM3Zi00YWE0LWExMzUtODk2MjQxZGQ4ZDcxIiDQndC+0LzQn9C+0LTRgtCyPSJlZWNiZmZlYy1lMzdmLTRhYTQtYTEzNS04OTYyNDFkZDhkNzEiINCU0LDRgtCw0JLRgNC10LzQktGL0LTQn9C+0LTRgtCyPSIyMDI1LTA0LTI3VDEyOjUwOjQxLjM2OTcyNzM4NCIg0JDQtNGA0J3QvtC00J7Qv9C10YA9IjNNN0VFbnN6UEFUMnlyNzJTZ1dWREx4ZllDYTRBWXZWUnd2Ij480KHQstCu0JvQn9C+0LTRgtCyINCY0LTQrtCbPSJkZThkMjg3NS0yODNhLTRmNTktOTI0Ni1lMTg3OWUxNDViYzIiINCd0LDQuNC80J7RgNCzPSLQpNC10LTQtdGA0LDQu9GM0L3QsNGPINC90LDQu9C+0LPQvtCy0LDRjyDRgdC70YPQttCx0LAiINCi0L7RgNCz0J3QsNC40Lw9ItCk0J3QoSDQoNC+0YHRgdC40LgiINCY0J3QndCu0Js9Ijc3MDczMjkxNTIiINCe0JPQoNCdPSIxMDQ3NzA3MDMwNTEzIj48VVJM0J/RgNC+0LTRg9C60YLQntC/0LXRgD5odHRwczovL3BvYXIuY3Byci1pdC53ZWludGVncmF0b3IuY29tPC9VUkzQn9GA0L7QtNGD0LrRgtCe0L/QtdGAPjzQktC10YDRgdCf0YDQvtCzPnBvYXItd2ViYXBwLWFwcCAxLjc3LjI8L9CS0LXRgNGB0J/RgNC+0LM+PC/QodCy0K7Qm9Cf0L7QtNGC0LI+PNCh0LLQl9Cw0Y/QstC70J7RgtC80LXQvdCwINCl0Y3RiNCX0LDRj9Cy0LvQntGC0LzQtdC90LA9InUzeWhwdUtEVUN0ZnIyYUNzSjF4dTdoRzFOUytBWXUrdldxZWhhVWcvVkE9IiDQndC+0LzQlNC+0LLQtdGAPSI4MzI1ZjU1Yy04MjIzLTQ3YjItOTNkYS1mZjkzNDFmMmM4NGMiLz48L9Cf0L7QtNGC0LLQtdGA0LbQtNC10L3QuNC1Pg==";
        String filenameSig = "/tmp/" + m4dRevokeConfirmDto.getIdFile()+"_detached.sig";
        tempFileNames.add(filenameSig);
        fo = new FileOutputStream(filenameSig);
        byte[] bytes1 = resource.getBytes();
        fo.write(bytes1);
        fo.close();
        return tempFileNames;
    }

    public M4dConfirmDto getXmlM4dConfirm(String guid, String internalNumber, String startDate, String endDate, String createDateTime) {
        String orgId = "de8d2875-283a-4f59-9246-e1879e145bc2";
        String orgName = "Федеральная налоговая служба";
        String orgTradeName = "ФНС России";
        String orgInn = "7707329152";
        String orgOgrn = "1047707030513";
        String orgInfoSystem = "https://poar.cprr-it.weintegrator.com";
        String orgVersionSystem = "poar-webapp-app 1.77.2";
        //
        String guidConfirm = UUID.randomUUID().toString();
        M4dConfirmDto m4dConfirmDto = new M4dConfirmDto();
        m4dConfirmDto.setVersionFormat("ПодтвФНС003");
        m4dConfirmDto.setTypeConfirm("ПодтвДовер");
        m4dConfirmDto.setIdFile("Квит003_ФНС_ON_EMCHD_"+guid); //Квит003_ФНС_ON_EMCHD_20240516_75655b80-cc58-4a2b-b244-8334eb895a2f
        m4dConfirmDto.setGuid(guidConfirm);
        m4dConfirmDto.setRegisteredDate(createDateTime);
        m4dConfirmDto.setOperatorId("3M7EEnszPAT2yr72SgWVDLxfYCa4AYvVRwv");
        M4dConfirmDto.Organisation organisation = new M4dConfirmDto.Organisation();
        organisation.setId(orgId);
        organisation.setName(orgName);
        organisation.setTradeName(orgTradeName);
        organisation.setInn(orgInn);
        organisation.setOgrn(orgOgrn);
        organisation.setInfoSystem(orgInfoSystem);
        organisation.setVersionSystem(orgVersionSystem);
        m4dConfirmDto.setOrganisation(organisation);
        M4dConfirmDto.M4d m4d = new M4dConfirmDto.M4d();
        m4d.setIdFile("ON_EMCHD_"+guid);
        m4d.setGuid(guid);
        m4d.setInternalNumber(internalNumber);
        m4d.setStartDate(startDate);
        m4d.setEndDate(endDate);
        m4d.setInfoSystem("https://m4d.nalog.gov.ru/EMCHD/check-status?guid="+guid);
        m4dConfirmDto.setM4d(m4d);
        return m4dConfirmDto;
    }

    public M4dRevokeConfirmDto getXmlM4dRevokeConfirm(String guid, String hashFile, String createDateTime) {
        String orgId = "de8d2875-283a-4f59-9246-e1879e145bc2";
        String orgName = "Федеральная налоговая служба";
        String orgTradeName = "ФНС России";
        String orgInn = "7707329152";
        String orgOgrn = "1047707030513";
        String orgInfoSystem = "https://poar.cprr-it.weintegrator.com";
        String orgVersionSystem = "poar-webapp-app 1.77.2";
        //
        String guidConfirm = UUID.randomUUID().toString();
        M4dRevokeConfirmDto m4dRevokeConfirmDto = new M4dRevokeConfirmDto();
        m4dRevokeConfirmDto.setVersionFormat("ПодтвФНС003");
        m4dRevokeConfirmDto.setTypeConfirm("ПодтвЗаявлОтмена");
        m4dRevokeConfirmDto.setIdFile("Квит003_ФНС_ЗаявОтм_"+guidConfirm); //Квит003_ФНС_ЗаявОтм_eecbffec-e37f-4aa4-a135-896241dd8d71
        m4dRevokeConfirmDto.setGuid(guidConfirm);
        m4dRevokeConfirmDto.setRegisteredDate(createDateTime);
        m4dRevokeConfirmDto.setOperatorId("3M7EEnszPAT2yr72SgWVDLxfYCa4AYvVRwv");
        M4dRevokeConfirmDto.Organisation organisation = new M4dRevokeConfirmDto.Organisation();
        organisation.setId(orgId);
        organisation.setName(orgName);
        organisation.setTradeName(orgTradeName);
        organisation.setInn(orgInn);
        organisation.setOgrn(orgOgrn);
        organisation.setInfoSystem(orgInfoSystem);
        organisation.setVersionSystem(orgVersionSystem);
        m4dRevokeConfirmDto.setOrganisation(organisation);
        M4dRevokeConfirmDto.M4dRevoke m4d = new M4dRevokeConfirmDto.M4dRevoke();
        m4d.setHashFile(hashFile);
        m4d.setGuid(guid);
        m4dRevokeConfirmDto.setM4d(m4d);
        return m4dRevokeConfirmDto;
    }

    public static String marshall(@NonNull final Object wrapper, @NonNull final Class<?> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(wrapper, stringWriter);
        return stringWriter.toString();
    }
}
