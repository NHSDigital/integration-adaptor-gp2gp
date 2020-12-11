package uk.nhs.adaptors.gp2gp.extension;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.container.MongoDbContainer;
import uk.nhs.adaptors.gp2gp.repositories.EhrExtractStatusRepository;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
public class MongoDBExtension implements BeforeAllCallback, BeforeEachCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        MongoDbContainer.getInstance().start();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);

        EhrExtractStatusRepository ehrExtractStatusRepository = applicationContext.getBean(EhrExtractStatusRepository.class);
        ehrExtractStatusRepository.deleteAll();
    }
}
