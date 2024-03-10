import com.github.tomakehurst.wiremock.extension.TemplateModelDataProviderExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.json.JSONObject;

import java.util.Map;

class PatientTemplateDataProvider implements TemplateModelDataProviderExtension {
    final PatientDemographicsServiceClient pds;

    PatientTemplateDataProvider(PatientDemographicsServiceClient pds) {
        this.pds = pds;
    }

    @Override
    public Map<String, Object> provideTemplateModelData(ServeEvent serveEvent) {
        try {
            var nhsNumber = getNhsNumber(serveEvent);
            if (nhsNumber != null) {
                System.out.println("Fetching patient details for " + nhsNumber);
                return Map.of("patient", pds.patient(nhsNumber));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Map.of();
    }

    private static String getNhsNumber(ServeEvent serveEvent) {
        try {
            JSONObject object = new JSONObject(serveEvent.getRequest().getBodyAsString());
            return object.getJSONArray("parameter").getJSONObject(0).getJSONObject("valueIdentifier").getString("value");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "personal-demographics-service";
    }
}
