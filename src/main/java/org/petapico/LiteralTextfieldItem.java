package org.petapico;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

public class LiteralTextfieldItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public LiteralTextfieldItem(String id, IRI iri, final PublishForm form) {
		super(id);
		IModel<String> model = form.formComponentModels.get(iri);
		if (model == null) {
			model = Model.of("");
			form.formComponentModels.put(iri, model);
		}
		TextField<String> textfield = new TextField<>("textfield", model);
		textfield.setRequired(true);
		if (form.template.getLabel(iri) != null) {
			textfield.add(new AttributeModifier("placeholder", form.template.getLabel(iri)));
		}
		form.formComponentModels.put(iri, textfield.getModel());
		form.formComponents.add(textfield);
		add(textfield);
	}

}
