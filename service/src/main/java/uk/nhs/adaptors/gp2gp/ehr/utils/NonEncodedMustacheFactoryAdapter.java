package uk.nhs.adaptors.gp2gp.ehr.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.MustacheVisitor;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.ValueCode;

public class NonEncodedMustacheFactoryAdapter extends DefaultMustacheFactory {
    public NonEncodedMustacheFactoryAdapter(String templatesDirectory) {
        super(templatesDirectory);
    }

    @Override
    public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
            @Override
            public void value(TemplateContext tc, String variable, boolean encoded) {
                list.add(new ValueCode(tc, df, variable, false));
            }
        };
    }
}
