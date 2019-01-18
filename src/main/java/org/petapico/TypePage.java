package org.petapico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TypePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public TypePage(final PageParameters parameters) {
		String typeId = parameters.get("id").toString();
		add(new Label("typeid", typeId));

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("type", typeId);
		List<String> nanopubUris = ApiAccess.getAll("find_latest_nanopubs_with_type", nanopubParams, 0);

		List<NanopubElement> nanopubs = new ArrayList<>();
		for (int i = 0 ; i < 10 && i < nanopubUris.size() ; i++) {
			nanopubs.add(new NanopubElement(nanopubUris.get(i)));
		}

		add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				item.add(new NanopubItem("nanopub", item.getModelObject()));
			}

		});
	}

}
