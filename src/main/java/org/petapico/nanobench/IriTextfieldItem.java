package org.petapico.nanobench;

import java.net.URISyntaxException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;

public class IriTextfieldItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	private String prefix;

	public IriTextfieldItem(String id, IRI iri, final PublishForm form) {
		super(id);
		IModel<String> model = form.formComponentModels.get(iri);
		if (model == null) {
			model = Model.of("");
			form.formComponentModels.put(iri, model);
		}
		prefix = form.template.getPrefix(iri);
		if (prefix == null) prefix = "";
		String prefixLabel = form.template.getPrefixLabel(iri);
		if (prefixLabel == null) prefixLabel = "";
		add(new Label("prefix", prefixLabel));
		final TextField<String> textfield = new TextField<>("textfield", model);
		textfield.setRequired(true);
		textfield.add(new IValidator<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void validate(IValidatable<String> s) {
				try {
					ParsedIRI piri = new ParsedIRI(prefix + s.getValue());
					if (!piri.isAbsolute()) {
						s.error(new ValidationError("IRI not well-formed"));
					}
				} catch (URISyntaxException ex) {
					s.error(new ValidationError("IRI not well-formed"));
				}
			}

		});
		form.formComponents.add(textfield);
		if (form.template.getLabel(iri) != null) {
			textfield.add(new AttributeModifier("placeholder", form.template.getLabel(iri)));
			textfield.setLabel(Model.of(form.template.getLabel(iri)));
		}
		textfield.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FormComponent<String> fc : form.formComponents) {
					if (fc == textfield) continue;
					if (fc.getModel() == textfield.getModel()) {
						fc.modelChanged();
						target.add(fc);
					}
				}
			}

		});
		add(textfield);
	}

}
