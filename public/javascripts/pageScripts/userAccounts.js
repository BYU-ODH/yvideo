$(function(){
    "use strict";
    var userList = getUsers(0, 50, true),
    userTemplate = '<SuperSelect\
        icon="" text="Select Permissions"\
        button="left"\
        value="{{activePermissions}}"\
        multiple="true"\
        options="{{permissionList}}"\
    />\
    <input id="searchVal" type="text" value="" placeholder="Search..."><br>\
    <input type="radio" id="username" value="username" name="radio" checked/>Username<br>\
    <input type="radio" id="name" value="name" name="radio"/>Name<br>\
    <input type="radio" id="email" value="email" name="radio"/>Email<br>\
    <button on-tap="searchUsers" class="btn btn-small">Search</button>\
    <table class="table table-bordered table-condensed">\
        <tr>\
            <th>Id</th><th>Last Login</th><th>Username</th><th>Name</th><th>Email</th>\
            <th>Account Type</th><th>Permissions</th><th>Actions</th>\
        </tr>\
        {{#users:index}}\
            <tr style="background: {{calc_color(.permissions, activePermissions)}};" id="uid{{.id}}">\
                <td><label><input type="checkbox" name="{{selectedUsers}}" value="{{index}}" id={{.id}}/>{{.id}}</label></td>\
                <td>{{.lastLogin.substr(0,10)}}</td><td>{{{shorten(.username)}}}</td>\
                <td>{{{(.name?shorten(.name):"<em>Not set</em>")}}}</td>\
                <td>{{{(.email?shorten(.email):"<em>Not set</em>")}}}</td>\
                <td>\
                    {{#is_missing(.permissions, activePermissions)}}<a on-tap="add:{{index}}" class="btn btn-small btn-yellow">Add Missing</a>{{/missing}}\
                    {{#has_extra(.permissions, activePermissions)}}<a on-tap="remove:{{index}}" class="btn btn-small btn-yellow">Remove Extra</a>{{/extra}}\
                    {{#(is_missing(.permissions, activePermissions) && has_extra(.permissions, activePermissions))}}\
                        <a on-tap="match:{{index}}" class="btn btn-small btn-yellow">Match Filter</a>\
                    {{/match}}\
                </td><td>\
                    {{#(.id !== viewer_id)}}\
                        <a on-tap="proxy:{{.id}}" class="btn btn-small btn-yellow">Proxy</a>\
                        <a on-tap="notify:{{.id}}" class="btn btn-small btn-blue sendNote">Notify</a>\
                        <a on-tap="delete:{{.id}}" class="btn btn-small btn-magenta deleteUser">Delete</a>\
                    {{/is_viewer}}\
                </td>\
            </tr>\
        {{/users}}\
        <tr><td colspan="8">\
            <b>For selected:</b>\
            <a on-tap="addsel" class="btn btn-small btn-yellow">Add Missing</a>\
            <a on-tap="removesel" class="btn btn-small btn-yellow">Remove Extra</a>\
            <a on-tap="matchsel" class="btn btn-small btn-yellow">Match Filter</a>\
            <a on-tap="notifysel" class="btn btn-small btn-blue">Notify</a>\
            <a on-tap="deletesel" class="btn btn-small btn-magenta deleteUser">Delete</a>\
        </td></tr>\
    </table>\
    <input id="limit" type="text" value="50" style="width: 35px; margin-top: 10px;">\
    <button on-tap="getPrev" class="btn btn-small btn-yellow">Previous</button>\
    <button on-tap="getNext" class="btn btn-small btn-yellow">Next</button>';

    /* Add back when setting the selection outside of superselect works:
                    {{#(is_missing(.permissions, activePermissions) || has_extra(.permissions, activePermissions))}}\
                        <a on-tap="select:{{index}}" class="btn btn-small btn-yellow">Select Permissions</a>\
                    {{/match}}\
    */

    var utable = new Ractive({
        el: document.getElementById('usertable'),
        template: userTemplate,
        data: {
            activePermissions: [],
            permissionList: userPermissionList,
            selectedUsers: [],
            users: userList,
            viewer_id: viewer_id,
            shorten: function(str) {
                // console.log(str);
                return str.length < 16?str:
                    '<span title="'+str+'">'+str.substr(0,12)+'...</span>';
            },
            is_missing: function(uperm, aperm){
                return aperm.some(function(p){ return uperm.indexOf(p) === -1; });
            },
            has_extra: function(uperm, aperm){
                return uperm.some(function(p){ return aperm.indexOf(p) === -1; });
            },
            calc_color: function(uperm, aperm){
                var missing = aperm.some(function(p){ return uperm.indexOf(p) === -1; }),
                    extra = uperm.some(function(p){ return aperm.indexOf(p) === -1; });
                return missing?
                    (extra?"#FF00FF":"#FFFFFF"):
                    (extra?"#AAAAFF":"#AAFFAA");
            },
            get_first_index: function(){
                // gets the smallest user id currently in the table -1 or 0
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

    utable.on('add', function(e,index){
        addUserPermissions(index).then(null,function(e){
            alert("Error updating permissions");
        });
    });
    utable.on('remove', function(e,index){
        removeUserPermissions(index).then(null,function(e){
            alert("Error updating permissions");
        });
    });
    utable.on('match', function(e,index){
        matchUserPermissions(index).then(null,function(e){
            alert("Error updating permissions");
        });
    });

    /*utable.on('select', function(e,index){
        utable.set('activePermissions', utable.get("users")[index].permissions);
    });*/

    function isNum(n) {
      return !isNaN(parseFloat(n)) && isFinite(n);
    }


    utable.on("getPrev", function(){
        var limit = $("#limit").val();
        var index = utable.get("get_first_index")();
        if (isNum(limit) && +limit > 0 && index >= 0) {
            getUsers(index, limit, false);
        } else console.log("error getting prev");
    });

    utable.on("getNext", function(){
        var limit = $("#limit").val();
        if (isNum(limit) && +limit > 0) {
            getUsers(utable.get("get_last_index")(), Math.abs(limit), true);
        } else {
        console.log("error getting next")
        };
    });

    utable.on("searchUsers", function (){
        var searchVal = $("#searchVal").val();
        var colName = document.querySelector('input[name = "radio"]:checked').value;
        getUsers(null, null, null, true, colName, searchVal);
    })

    utable.on('proxy', function(e,id){
        window.location = "/admin/proxy/"+id;
    });
    utable.on('notify', function(e,id){
        $("#noteUserId").val(id);
        $("#noteUserIdDisplay").text(id);
        $("#notificationModal").modal('show');
    });
    utable.on('delete', function(e,id){
        $("#deleteUserId").text(id);
        $("#deleteForm").attr("action", "/admin/users/" + id + "/delete");
        $("#deleteModal").modal('show');
    });

    utable.on('addsel', function(){
        Promise.all(utable.get("selectedUsers").map(addUserPermissions))
        .then(null,function(e){
            alert("Error updating permissions");
        });
    });
    utable.on('removesel', function(){
        utable.get("selectedUsers").map(removeUserPermissions)
        .then(null,function(e){
            alert("Error updating permissions");
        });
    });
    utable.on('matchsel', function(){
        utable.get("selectedUsers").map(matchUserPermissions)
        .then(null,function(e){
            alert("Error updating permissions");
        });
    });

    utable.on('deletesel', function(){
        if (utable.get("selectedUsers").length == 0) {
            alert("Nothing selected");
        } else {
            //TODO convert to bootstrap modal instead of JavaScript confirm
            var cont = confirm("Are you sure you want to delete the selected users?");
            cont;
            if (cont == true) {
                utable.get("selectedUsers").forEach(deleteUsers);
            }
        }
    });

    utable.on('notifysel', function(){
        if (utable.get("selectedUsers").length == 0) {
            alert("Nothing selected");
        } else {
            utable.get("selectedUsers").forEach(notifyUsers);
        }
    });

    function deleteUsers(index){
        var userId = utable.get("users")[index].id;
        var request = new XMLHttpRequest();
        request.open("POST", "/admin/users/"+userId+"/delete");
        request.send();
        location.reload();
    }

    //TODO
    function notifyUsers(index){
        var userId = utable.get("users")[index].id;
        alert(userId);
    }

    /**
     * Does an AJAX request for getting paginated users for admin dashbaord.
     */
    function getUsers(index, limit, up, search, columnName, searchValue){
        var request = new XMLHttpRequest();
        request.onreadystatechange = function(){
            if (request.readyState == 4 && request.status == 200) {
              var users = JSON.parse(request.responseText);
              if(users.length > 0) { utable.set("users", users); }
            }
        }
        if (search) {
            request.open("GET", "/admin/users/"+columnName+"/"+searchValue, true);
            request.send();
        } else {
            request.open("GET", "/admin/users/"+index+"/"+limit+"/"+up, true);
            request.send();    
        }
    }

    function updateUserPermissions(index, perms, operation){
        var data = new FormData();
        data.append("userId", utable.get("users")[index].id);
        perms.forEach(function(p){ data.append("permission", p); });
        return Promise.resolve($.ajax({
            url: permUrl + (operation || ""),
            data: data,
            cache: false,
            contentType: false,
            processData: false,
            type: "post",
            dataType: "text"
        })).then(function(data){
            switch(operation) {
                case "remove":
                    var uperm = utable.get('users['+index+'].permissions');
                    perms.forEach(function(p) { uperm.splice(uperm.indexOf(p), 1); });
                    break;
                case "match":
                    utable.set('users['+index+'].permissions', perms);
                    break;
                case "add":
                    var uperm = utable.get('users['+index+'].permissions');
                    perms.forEach(function(p){ uperm.push(p); });
                    break;
            }
        });
    }


    function addUserPermissions(index){
        var aperm = utable.get('activePermissions'),
            uperm = utable.get('users['+index+'].permissions'),
            nperm = aperm.filter(function(p){ return uperm.indexOf(p) === -1; });
        if(nperm.length === 0){ return Promise.resolve(true); }
        [].push.apply(nperm, uperm);
        return updateUserPermissions(index, nperm, 'add');
    }

    function removeUserPermissions(index){
        var aperm = utable.get('activePermissions'),
            uperm = utable.get('users['+index+'].permissions'),
            // gets all permissions the user has which are not active
            nperm = uperm.filter(function(p){ return aperm.indexOf(p) === -1; });
        // updates the permissions if there are any to update (in nperm)
        if(nperm.length === 0){ return Promise.resolve(true); }
        return updateUserPermissions(index, nperm, 'remove');
    }

    function matchUserPermissions(index){
        var aperm = utable.get('activePermissions'),
            uperm = utable.get('users['+index+'].permissions');
        if(aperm.length === uperm.length &&
            aperm.every(function(p){ return uperm.indexOf(p) > -1; }) &&
            uperm.every(function(p){ return aperm.indexOf(p) > -1; })
        ){ return Promise.resolve(true); }
        return updateUserPermissions(index, aperm, 'match');
    }
});
