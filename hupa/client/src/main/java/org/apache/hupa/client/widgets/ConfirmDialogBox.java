/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.hupa.client.widgets;

import org.apache.hupa.client.HupaConstants;
import org.cobogw.gwt.user.client.ui.Button;
import org.cobogw.gwt.user.client.ui.ButtonBar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ConfirmDialogBox extends MyDialogBox implements HasClickHandlers {
    private HupaConstants constants = GWT.create(HupaConstants.class);
    private Label text = new Label();
    private ButtonBar bar = new ButtonBar();
    private VerticalPanel panel = new VerticalPanel();
    private Button okButton = new Button(constants.okButton());
    private Button cancelButton = new Button(constants.cancelButton());
    
    public ConfirmDialogBox() {    
        super();
        setModal(true);
        setAnimationEnabled(true);  
        setAutoHideEnabled(false);
        super.setText(constants.productName());
        panel.setSpacing(10);
        panel.add(text);
        bar.add(okButton);
        bar.add(cancelButton);
        panel.add(bar);
        panel.setCellHorizontalAlignment(bar, VerticalPanel.ALIGN_RIGHT);
        
        cancelButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                hide();
            }
            
        });
        okButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                hide();
            }
            
        });
        add(panel);
    }
    
    public void setText(String value) {
        text.setText(value);
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return okButton.addClickHandler(handler);
    }
    
    public void show() {
        super.show();
        center();

    }

}
