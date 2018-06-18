/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *
 *  Contributors:
 *
 *     IBM Corporation - initial API and implementation
 *     Jim Amsden      - Uses a tree view to lazily navigate the IoT Platform resource hierarchy for selection
 *
 *******************************************************************************/

require([
    "dojo/_base/window", "dojo/store/Memory",
    "dijit/tree/ObjectStoreModel", "dijit/Tree",
    "dojo/domReady!"
], function(win, Memory, ObjectStoreModel, Tree){	
    // Create the platform data store, adding the getChildren() method required by ObjectStoreModel
    var iotStore = new Memory({
        data: tableData,  // filled in by the selection dialog from the connector manager resources
        getChildren: function(object){
            return this.query({parent: object.id});
        }
    });

    // Create the tree view model
    var iotModel = new ObjectStoreModel({
        store: iotStore,
        query: {id: 'root'}  // root is not displayed
    });

    // Create the Tree.
    tree = new Tree({
        model: iotModel,
        showRoot: false
    });
    treeView =  document.getElementById("treeView")
    tree.placeAt(treeView);
    
    // Handle selection events to lazily load child elements
    tree.onClick = function(item, node, evt) {
    		var selectedItems = ""
    		for (var item in tree.selectedItems) {
    			selectedItems = selectedItems + tree.selectedItems[item].name + " "
    		}
    }
    tree.startup();
});

// Called when the OK button is pressed
function select() {
	var oslcResponse = 'oslc-response:{ "oslc:results": [ '
		for (var item in tree.selectedItems) {
			oslcResponse += ' { "oslc:label" : "' + tree.selectedItems[item].name + '", "rdf:resource" : "' + tree.selectedItems[item].id + '"}'
			if (item < tree.selectedItems.length-1) oslcResponse += ','
		}
		oslcResponse += ']}'
		if (window.location.hash == '#oslc-core-windowName-1.0') {
		    // Window Name protocol in use
		    respondWithWindowName(oslcResponse);
		} else if (window.location.hash == '#oslc-core-postMessage-1.0') {
		    // Post Message protocol in use
		    respondWithPostMessage(oslcResponse);
		}
}


function respondWithWindowName(/*string*/ response) {
   var returnURL = window.name;
   window.name = response;
   window.location.href = returnURL;

}

function respondWithPostMessage(/*string*/ response) {
  if( window.parent != null ) {
    window.parent.postMessage(response, "*");
  } else {
    window.postMessage(response, "*");
  }
}

function cancel(){
  sendCancelResponse();
}
