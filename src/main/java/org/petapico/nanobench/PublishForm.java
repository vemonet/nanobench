package org.petapico.nanobench;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubWithNs;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.server.PublishNanopub;
import org.petapico.nanobench.PublishFormContext.ContextType;

import net.trustyuri.TrustyUriException;

public class PublishForm extends Panel {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	protected Form<?> form;
	protected FeedbackPanel feedbackPanel;
	private final PublishFormContext assertionContext, provenanceContext;

	public PublishForm(String id, PageParameters pageParams, final PublishPage page) {
		super(id);
		final String templateId = pageParams.get("template").toString();
		final String prTemplateId = "http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM";

		Map<String,String> params = new HashMap<String,String>();
		Map<String,String> prParams = new HashMap<String,String>();
		Map<String,String> piParams = new HashMap<String,String>();
		for (String k : pageParams.getNamedKeys()) {
			if (k.startsWith("param_")) params.put(k.substring(6), pageParams.get(k).toString());
			if (k.startsWith("prparam_")) prParams.put(k.substring(8), pageParams.get(k).toString());
			if (k.startsWith("piparam_")) piParams.put(k.substring(8), pageParams.get(k).toString());
		}
		assertionContext = new PublishFormContext(ContextType.ASSERTION, templateId);
		provenanceContext = new PublishFormContext(ContextType.PROVENANCE, prTemplateId);

		List<Panel> statementItems = assertionContext.makeStatementItems("statement");
		List<Panel> provStatementItems = provenanceContext.makeStatementItems("pr-statement");

		final CheckBox consentCheck = new CheckBox("consentcheck", new Model<>(false));
		consentCheck.setRequired(true);
		consentCheck.add(new IValidator<Boolean>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void validate(IValidatable<Boolean> validatable) {
				if (!Boolean.TRUE.equals(validatable.getValue())) {
					validatable.error(new ValidationError("You need to check the checkbox that you understand the consequences."));
				}
			}
			
		});

		form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				form.getFeedbackMessages().clear();
//				formComponents.clear();
			}

			protected void onSubmit() {
				try {
					Nanopub np = createNanopub();
					Nanopub signedNp = SignNanopub.signAndTransform(np, SignatureAlgorithm.RSA, ProfilePage.getKeyPair());
					if (templateId.startsWith("file://") || prTemplateId.startsWith("file://")) {
						// Testing mode
						System.err.println("This nanopublication would have been published (if we were not in testing mode):");
						System.err.println("----------");
						System.err.println(org.nanopub.NanopubUtils.writeToString(signedNp, org.eclipse.rdf4j.rio.RDFFormat.TRIG));
						System.err.println("----------");
					} else {
						PublishNanopub.publish(signedNp);
						System.err.println("Published " + signedNp.getUri());
					}
					PageParameters params = new PageParameters();
					params.add("id", ProfilePage.getUserIri().stringValue());
					throw new RestartResponseException(new PublishConfirmPage(signedNp));
				} catch (IOException | MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
					ex.printStackTrace();
				}
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				for (FormComponent<String> fc : assertionContext.getFormComponents()) {
					fc.processInput();
					for (FeedbackMessage fm : fc.getFeedbackMessages()) {
						form.getFeedbackMessages().add(fm);
					}
				}
			}

		};

		form.add(new ExternalLink("templatelink", templateId));
		form.add(new Label("templatename", assertionContext.getTemplate().getLabel()));

		form.add(new ListView<Panel>("statements", statementItems) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<Panel> item) {
				item.add(item.getModelObject());
			}

		});

		form.add(new ExternalLink("prtemplatelink", prTemplateId));
		form.add(new Label("prtemplatename", provenanceContext.getTemplate().getLabel()));

		form.add(new ListView<Panel>("pr-statements", provStatementItems) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<Panel> item) {
				item.add(item.getModelObject());
			}

		});

		form.add(consentCheck);
		add(form);

		if (templateId.startsWith("file://") || prTemplateId.startsWith("file://")) {
			add(new Link<Object>("local-reload-link") {
				private static final long serialVersionUID = 1L;
				public void onClick() {
					setResponsePage(page.getPageClass(), page.getPageParameters());
				};
			});
			form.add(new Label("local-file-text", "TEST MODE. Nanopublication will not actually be published."));
		} else {
			Label l = new Label("local-reload-link", "");
			l.setVisible(false);
			add(l);
			form.add(new Label("local-file-text", ""));
		}

		feedbackPanel = new FeedbackPanel("feedback");
		feedbackPanel.setOutputMarkupId(true);
		add(feedbackPanel);
	}

	public static final IRI INTRODUCES_PREDICATE = vf.createIRI("http://purl.org/nanopub/x/introduces");

	private synchronized Nanopub createNanopub() throws MalformedNanopubException {
		Template template = assertionContext.getTemplate();
		assertionContext.getIntroducedIris().clear();
		NanopubCreator npCreator = new NanopubCreator(PublishFormContext.NP_TEMP_IRI);
		npCreator.setAssertionUri(PublishFormContext.ASSERTION_TEMP_IRI);
		if (template.getNanopub() instanceof NanopubWithNs) {
			NanopubWithNs np = (NanopubWithNs) template.getNanopub();
			for (String p : np.getNsPrefixes()) {
				npCreator.addNamespace(p, np.getNamespace(p));
			}
		}
		npCreator.addNamespace("this", "http://purl.org/nanopub/temp/nanobench-new-nanopub/");
		npCreator.addNamespace("sub", "http://purl.org/nanopub/temp/nanobench-new-nanopub/#");
		assertionContext.propagateStatements(npCreator);
		provenanceContext.propagateStatements(npCreator);
		for (IRI introducedIri : assertionContext.getIntroducedIris()) {
			npCreator.addPubinfoStatement(INTRODUCES_PREDICATE, introducedIri);
		}
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(SimpleCreatorPattern.DCT_CREATOR, ProfilePage.getUserIri());
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, template.getNanopub().getUri());
		return npCreator.finalizeNanopub();
	}

}
