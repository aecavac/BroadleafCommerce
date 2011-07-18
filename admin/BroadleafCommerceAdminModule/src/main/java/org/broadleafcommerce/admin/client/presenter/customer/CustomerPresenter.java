/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.broadleafcommerce.admin.client.presenter.customer;

import java.util.HashMap;
import java.util.Map;

import org.broadleafcommerce.admin.client.CustomerCareModule;
import org.broadleafcommerce.admin.client.datasource.customer.ChallengeQuestionListDataSourceFactory;
import org.broadleafcommerce.admin.client.datasource.customer.CustomerListDataSourceFactory;
import org.broadleafcommerce.admin.client.view.customer.CustomerDisplay;
import org.broadleafcommerce.openadmin.client.BLCMain;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.AbstractDynamicDataSource;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.DynamicEntityDataSource;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.ListGridDataSource;
import org.broadleafcommerce.openadmin.client.dto.Entity;
import org.broadleafcommerce.openadmin.client.dto.OperationType;
import org.broadleafcommerce.openadmin.client.dto.OperationTypes;
import org.broadleafcommerce.openadmin.client.dto.PersistencePerspective;
import org.broadleafcommerce.openadmin.client.dto.Property;
import org.broadleafcommerce.openadmin.client.event.NewItemCreatedEvent;
import org.broadleafcommerce.openadmin.client.event.NewItemCreatedEventHandler;
import org.broadleafcommerce.openadmin.client.presenter.entity.DynamicEntityPresenter;
import org.broadleafcommerce.openadmin.client.reflection.Instantiable;
import org.broadleafcommerce.openadmin.client.service.AbstractCallback;
import org.broadleafcommerce.openadmin.client.service.AppServices;
import org.broadleafcommerce.openadmin.client.setup.AsyncCallbackAdapter;
import org.broadleafcommerce.openadmin.client.setup.PresenterSetupItem;
import org.broadleafcommerce.openadmin.client.view.dynamic.dialog.EntitySearchDialog;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

/**
 * 
 * @author jfischer
 *
 */
public class CustomerPresenter extends DynamicEntityPresenter implements Instantiable {
	
	protected HashMap<String, Object> library = new HashMap<String, Object>();
	
	@Override
	protected void changeSelection(final Record selectedRecord) {
		getDisplay().getUpdateLoginButton().enable();
	}
	
	@Override
	protected void addClicked() {
		Map<String, Object> initialValues = new HashMap<String, Object>();
		initialValues.put("username", CustomerCareModule.ADMINMESSAGES.usernameDefault());
		initialValues.put("_type", new String[]{((DynamicEntityDataSource) display.getListDisplay().getGrid().getDataSource()).getDefaultNewEntityFullyQualifiedClassname()});
		BLCMain.ENTITY_ADD.editNewRecord(CustomerCareModule.ADMINMESSAGES.newCustomerTitle(), (DynamicEntityDataSource) display.getListDisplay().getGrid().getDataSource(), initialValues, new NewItemCreatedEventHandler() {
			public void onNewItemCreated(NewItemCreatedEvent event) {
				Criteria myCriteria = new Criteria();
				myCriteria.addCriteria("username", event.getRecord().getAttribute("username"));
				display.getListDisplay().getGrid().fetchData(myCriteria);
			}
		}, "90%", null, null);
	}

	@Override
	public void bind() {
		super.bind();
		getDisplay().getUpdateLoginButton().addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (event.isLeftButtonDown()) {
					SC.confirm(CustomerCareModule.ADMINMESSAGES.confirmResetPassword(), new BooleanCallback() {
						public void execute(Boolean value) {
							if (value) {
								BLCMain.NON_MODAL_PROGRESS.startProgress();
								
								PersistencePerspective tempPerspective = new PersistencePerspective();
			            		OperationTypes opTypes = new OperationTypes();
			            		opTypes.setUpdateType(OperationType.ENTITY);
			            		tempPerspective.setOperationTypes(opTypes);
			            		
								final Entity entity = new Entity();
			            		Property prop = new Property();
			            		prop.setName("username");
			            		prop.setValue(display.getListDisplay().getGrid().getSelectedRecord().getAttribute("username"));
			            		entity.setProperties(new Property[]{prop});
			            		entity.setType(new String[]{"org.broadleafcommerce.profile.core.domain.Customer"});
			            		
			            		AppServices.DYNAMIC_ENTITY.update(entity, tempPerspective, ((AbstractDynamicDataSource) display.getListDisplay().getGrid().getDataSource()).createSandBoxInfo(), new String[]{"passwordUpdate"}, new AbstractCallback<Entity>() {
									public void onSuccess(Entity arg0) {
										BLCMain.NON_MODAL_PROGRESS.stopProgress();
										SC.say(CustomerCareModule.ADMINMESSAGES.resetPasswordSuccessful());
									}	
			            		}); 
							}
						}
					});
				}
			}
		});
	}

	public void setup() {
		getPresenterSequenceSetupManager().addOrReplaceItem(new PresenterSetupItem("customerDS", new CustomerListDataSourceFactory(), null, new Object[]{}, new AsyncCallbackAdapter() {
			public void onSetupSuccess(DataSource top) {
				setupDisplayItems(top);
				((ListGridDataSource) top).setupGridFields(new String[]{"username", "firstName", "lastName", "emailAddress"}, new Boolean[]{true, true, true, true});
				library.put("customerDS", top);
			}
		}));
		getPresenterSequenceSetupManager().addOrReplaceItem(new PresenterSetupItem("challengeQuestionDS", new ChallengeQuestionListDataSourceFactory(), null, new Object[]{}, new AsyncCallbackAdapter() {
			public void onSetupSuccess(DataSource result) {
				((ListGridDataSource) result).resetPermanentFieldVisibility(
						"question"
					);
					final EntitySearchDialog challengeQuestionSearchView = new EntitySearchDialog((ListGridDataSource) result);
					
					((DynamicEntityDataSource) library.get("customerDS")).
					getFormItemCallbackHandlerManager().addSearchFormItemCallback(
						"challengeQuestion", 
						challengeQuestionSearchView, 
						CustomerCareModule.ADMINMESSAGES.challengeQuestionSearchPrompt(), 
						display.getDynamicFormDisplay()
					);
			}
		}));
	}

	@Override
	public CustomerDisplay getDisplay() {
		return (CustomerDisplay) display;
	}
	
}
