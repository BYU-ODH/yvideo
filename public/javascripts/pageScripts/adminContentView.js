$(function(){
	"use strict";
	var contentList = getContentList(0, 50, false),
	contentTemplate = '<table class="table table-bordered table-striped table-condensed adminTable">\
            <tr>\
                <th></th>\
                <th>Id</th>\
                <th>Name</th>\
                <th>Owner</th>\
                <th>Language</th>\
                <th>Visibility</th>\
                <th>Resource Id</th>\
                <th>Type</th>\
                <th>Actions</th>\
            </tr>\
            {{#content:index}}\
	            <tr id="{{.resourceId}}">\
	            	<td>\
	                    <input type="checkbox" class="contentSelector" data-id="">\
	                </td>\
	                <td>{{.contentId}}</td>\
	                <td>{{.cname}}</td>\
	                <td data-toggle="tooltip" >{{.username}}</td>\
	                <td class="lang"></td>\
	                <td>{{.visibility}}</td>\
	                <td>{{.resourceId}}</td>\
	                <td>{{.contentType}}</td>\
	                <td>\
	                	<a target="_blank" href="/content/{{.contentId}}" class="btn btn-gray">View</a>\
                    	<button on-tap="delete:{{.contentId}}" class="btn btn-magenta">Delete</button>\
	                </td>\
	            </tr>\
	        {{/content}}\
        </table>';

    var utable = new Ractive({
    	el: document.getElementById('contentTable'),
    	template: contentTemplate,
    	data: {
    		selectedContent: [],
    		content: contentList,
    		get_first_index: function(){
                // gets the smallest content id currently in the table -1 or 0
                if (utable.get("users").length > 0) {
                    return utable.get("users")[0].id-1>-1?utable.get("users")[0].id-1:0;
                } else {
                    return 0;
                }
            },
            get_last_index: function(){
                if (utable.get("users").length > 0) {
                    return utable.get("users")[utable.get("users").length-1].id+1 || 0;
                } else {
                    return 0;
                }
            }
    	}
    });

    utable.on('delete', function(e, index){
    	//TODO convert to bootstrap modal instead of JavaScript confirm
        var cont = confirm("Are you sure you want to delete this content?");
        cont;
        if (cont == true) {
			deleteContent(index);
        }
    });

    function deleteContent(id){
    	var request = new XMLHttpRequest();
    	request.open("POST", "/content/"+id+"/delete");
    	request.send();
    	location.reload();
    }

    function getContentList(index, limit, up, search, columnName, searchValue){
    	var request = new XMLHttpRequest();
        request.onreadystatechange = function(){
            if (request.readyState == 4 && request.status == 200) {
              var content = JSON.parse(request.responseText);
              if(content.length > 0) { utable.set("content", content); }
            }
        }
        if (search) {
            request.open("GET", "/"+columnName+"/"+searchValue, true);
            request.send();
        } else {
            request.open("GET", "/admin/content/"+index+"/"+limit+"/"+up, true);
            request.send();    
        }
    }
});