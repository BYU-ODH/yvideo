angular.module("editModule", [])
.service("linkedCourses", function() {
    // basically just a shared array
    var courses = [];

    var coursesEqual = function(c1, c2) {
        if (c1.department === c2.department &&
            c1.catalogNumber === c2.catalogNumber &&
            c1.sectionNumber === c2.sectionNumber) {
            return true;
        }
        else {
            return false;
        }
    };

    var getCourseName = function(course) {
        return (course.department + " " + !!course.catalogNumber?course.catalogNumber:"" + " " +
                !!course.sectionNumber?course.sectionNumber:"").trim();
    };

    return {
        get: function() {
            return courses;
        },
        contains: function(course) {
            if (typeof course === "string") {
                return courses.some(function(c){return getCourseName(c) === course.trim();});
            } else if (typeof course === "object") {
                return courses.some(function(c){return coursesEqual(course, c);});
            } else return false;
        },
        append: function(value) {
            if (value == null) return courses;
            if (value instanceof Array) {
                value.forEach(function(course) {
                    courses.push(course)
                });
            } else {
                courses.push(value);
            }
            return courses;
        },
        set: function(value) {
            if (value instanceof Array) {
                courses = value;
            }
            return courses;
        },
        remove: function(value) {
            if (value instanceof Array) {
                courses = courses.filter(function(course) {
                    return !~value.indexOf(course);
                });
            } else {
                // TODO: check if object
                // TODO: check the object for the required fields
                var index = courses.indexOf(course);
                courses.splice(index, !~index?1:0);
            }
        },
        // Copy object without the given key
        omit: function(obj, omitKey) {
            return Object.keys(obj).reduce(function(accumulator, currentKey) {
                if (currentKey !== omitKey) {
                    accumulator[currentKey] = obj[currentKey];
                }
                return accumulator;
            }, {});
        }
    }
})
.controller("editController", function($scope){
    $scope.selected = "removeContent";
})
.controller("addCourseController", function($scope, linkedCourses){
    $scope.courses = [];
    $scope.catalogNumber = "";
    $scope.sectionNumber = "";
    $scope.departmentList = [{"value":"A HTG","text":"A HTG - American Heritage"},{"value":"ACC","text":"ACC - Accounting"},{"value":"AEROS","text":"AEROS - Aerospace Studies"},{"value":"AM ST","text":"AM ST - American Studies"},{"value":"ANES","text":"ANES - Ancient Near Eastern Studies"},{"value":"ANTHR","text":"ANTHR - Anthropology"},{"value":"ARAB","text":"ARAB - Arabic"},{"value":"ART","text":"ART - Art"},{"value":"ARTED","text":"ARTED - Art Education"},{"value":"ARTHC","text":"ARTHC - Art History and Curatorial Studies"},{"value":"ASIAN","text":"ASIAN - Asian Studies"},{"value":"ASL","text":"ASL - American Sign Language"},{"value":"BIO","text":"BIO - Biology"},{"value":"BUS M","text":"BUS M - Business Management"},{"value":"C S","text":"C S - Computer Science"},{"value":"CANT","text":"CANT - Cantonese"},{"value":"CE EN","text":"CE EN - Civil and Environmental Engineering"},{"value":"CEBU","text":"CEBU - Cebuano"},{"value":"CFM","text":"CFM - Construction and Facilities Management"},{"value":"CH EN","text":"CH EN - Chemical Engineering"},{"value":"CHEM","text":"CHEM - Chemistry and Biochemistry"},{"value":"CHIN","text":"CHIN - Chinese - Mandarin"},{"value":"CL CV","text":"CL CV - Classical Civilization"},{"value":"CLSCS","text":"CLSCS - Classics"},{"value":"CMLIT","text":"CMLIT - Comparative Literature"},{"value":"CMPST","text":"CMPST - Comparative Studies"},{"value":"COMD","text":"COMD - Communication Disorders"},{"value":"COMMS","text":"COMMS - Communications"},{"value":"CPSE","text":"CPSE - Counseling Psychology and Special Education"},{"value":"CREOL","text":"CREOL - Haitian Creole"},{"value":"CSANM","text":"CSANM - Computer Science Animation"},{"value":"CZECH","text":"CZECH - Czech"},{"value":"DANCE","text":"DANCE - Dance"},{"value":"DANSH","text":"DANSH - Danish"},{"value":"DES","text":"DES - Design"},{"value":"DESAN","text":"DESAN - Design - Animation"},{"value":"DESGD","text":"DESGD - Design - Graphic Design"},{"value":"DESIL","text":"DESIL - Design - Illustration"},{"value":"DESPH","text":"DESPH - Design - Photography"},{"value":"DIGHT","text":"DIGHT - Digital Humanities and Technology"},{"value":"DUTCH","text":"DUTCH - Dutch"},{"value":"EC EN","text":"EC EN - Electrical and Computer Engineering"},{"value":"ECE","text":"ECE - Early Childhood Education"},{"value":"ECON","text":"ECON - Economics"},{"value":"EDLF","text":"EDLF - Educational Leadership and Foundations"},{"value":"EIME","text":"EIME - Educational Inquiry, Measurement, and Evaluation"},{"value":"EL ED","text":"EL ED - Elementary Education"},{"value":"ELANG","text":"ELANG - English Language"},{"value":"EMBA","text":"EMBA - Executive Master of Business Administration"},{"value":"ENG T","text":"ENG T - Engineering Technology"},{"value":"ENGL","text":"ENGL - English"},{"value":"ESTON","text":"ESTON - Estonian"},{"value":"EUROP","text":"EUROP - European Studies"},{"value":"EXSC","text":"EXSC - Exercise Sciences"},{"value":"FHSS","text":"FHSS - Family, Home, and Social Sciences"},{"value":"FIJI","text":"FIJI - Fijian"},{"value":"FIN","text":"FIN - Finance"},{"value":"FINN","text":"FINN - Finnish"},{"value":"FLANG","text":"FLANG - Foreign Language Courses"},{"value":"FNART","text":"FNART - Fine Arts"},{"value":"FREN","text":"FREN - French"},{"value":"GEOG","text":"GEOG - Geography"},{"value":"GEOL","text":"GEOL - Geological Sciences"},{"value":"GERM","text":"GERM - German"},{"value":"GREEK","text":"GREEK - Greek (Classical)"},{"value":"HAWAI","text":"HAWAI - Hawaiian"},{"value":"HCOLL","text":"HCOLL - Humanities College"},{"value":"HEB","text":"HEB - Hebrew"},{"value":"HINDI","text":"HINDI - Hindi"},{"value":"HIST","text":"HIST - History"},{"value":"HLTH","text":"HLTH - Health Science"},{"value":"HMONG","text":"HMONG - Hmong"},{"value":"HONRS","text":"HONRS - Honors Program"},{"value":"HUNG","text":"HUNG - Hungarian"},{"value":"IAS","text":"IAS - International and Area Studies"},{"value":"ICLND","text":"ICLND - Icelandic"},{"value":"ICS","text":"ICS - International Cinema Studies"},{"value":"IHUM","text":"IHUM - Interdisciplinary Humanities"},{"value":"INDES","text":"INDES - Industrial Design"},{"value":"INDON","text":"INDON - Indonesian"},{"value":"IP&T","text":"IP&T - Instructional Psychology and Technology"},{"value":"IS","text":"IS - Information Systems"},{"value":"IT","text":"IT - Information Technology"},{"value":"ITAL","text":"ITAL - Italian"},{"value":"JAPAN","text":"JAPAN - Japanese"},{"value":"KICHE","text":"KICHE - K'iche"},{"value":"KOREA","text":"KOREA - Korean"},{"value":"LATIN","text":"LATIN - Latin (Classical)"},{"value":"LATVI","text":"LATVI - Latvian"},{"value":"LAW","text":"LAW - Law"},{"value":"LFSCI","text":"LFSCI - Life Sciences"},{"value":"LING","text":"LING - Linguistics"},{"value":"LITHU","text":"LITHU - Lithuanian"},{"value":"LT AM","text":"LT AM - Latin American Studies"},{"value":"M COM","text":"M COM - Management Communication"},{"value":"MATH","text":"MATH - Mathematics"},{"value":"MBA","text":"MBA - Business Administration"},{"value":"ME EN","text":"ME EN - Mechanical Engineering"},{"value":"MESA","text":"MESA - Middle East Studies/Arabic"},{"value":"MFG","text":"MFG - Manufacturing"},{"value":"MFHD","text":"MFHD - Marriage, Family, and Human Development"},{"value":"MFT","text":"MFT - Marriage and Family Therapy"},{"value":"MIL S","text":"MIL S - Military Science"},{"value":"MMBIO","text":"MMBIO - Microbiology and Molecular Biology"},{"value":"MONGO","text":"MONGO - Mongolian"},{"value":"MPA","text":"MPA - Public Management"},{"value":"MTHED","text":"MTHED - Mathematics Education"},{"value":"MUSIC","text":"MUSIC - Music"},{"value":"NAVAJ","text":"NAVAJ - Navajo"},{"value":"NDFS","text":"NDFS - Nutrition, Dietetics, and Food Science"},{"value":"NE LG","text":"NE LG - Near Eastern Languages"},{"value":"NES","text":"NES - Near Eastern Studies"},{"value":"NEURO","text":"NEURO - Neuroscience"},{"value":"NORWE","text":"NORWE - Norwegian"},{"value":"NURS","text":"NURS - Nursing"},{"value":"ORG B","text":"ORG B - Organizational Behavior"},{"value":"PDBIO","text":"PDBIO - Physiology and Developmental Biology"},{"value":"PERSI","text":"PERSI - Persian"},{"value":"PETE","text":"PETE - Physical Education Teacher Education"},{"value":"PHIL","text":"PHIL - Philosophy"},{"value":"PHSCS","text":"PHSCS - Physics and Astronomy"},{"value":"PHY S","text":"PHY S - Physical Science"},{"value":"POLI","text":"POLI - Political Science"},{"value":"PORT","text":"PORT - Portuguese"},{"value":"PSYCH","text":"PSYCH - Psychology"},{"value":"PWS","text":"PWS - Plant and Wildlife Sciences"},{"value":"QUECH","text":"QUECH - Quechua"},{"value":"RECM","text":"RECM - Recreation Management"},{"value":"REL A","text":"REL A - Rel A - Ancient Scripture"},{"value":"REL C","text":"REL C - Rel C - Church History and Doctrine"},{"value":"REL E","text":"REL E - Rel E - Religious Education"},{"value":"ROM","text":"ROM - Romanian"},{"value":"RUSS","text":"RUSS - Russian"},{"value":"SAMOA","text":"SAMOA - Samoan"},{"value":"SC ED","text":"SC ED - Secondary Education"},{"value":"SCAND","text":"SCAND - Scandinavian Studies"},{"value":"SFL","text":"SFL - School of Family Life"},{"value":"SLAT","text":"SLAT - Second Language Teaching"},{"value":"SLOVK","text":"SLOVK - Slovak"},{"value":"SOC","text":"SOC - Sociology"},{"value":"SOC W","text":"SOC W - Social Work"},{"value":"SPAN","text":"SPAN - Spanish"},{"value":"STAC","text":"STAC - Student Activities"},{"value":"STAT","text":"STAT - Statistics"},{"value":"STDEV","text":"STDEV - Student Development"},{"value":"SWAHI","text":"SWAHI - Swahili"},{"value":"SWED","text":"SWED - Swedish"},{"value":"T ED","text":"T ED - Teacher Education"},{"value":"TAGAL","text":"TAGAL - Filipino, Tagalog"},{"value":"TECH","text":"TECH - Technology"},{"value":"TELL","text":"TELL - Teaching English Language Learners"},{"value":"TES","text":"TES - Technology and Engineering Studies"},{"value":"TEST","text":"TEST - Test"},{"value":"THAI","text":"THAI - Thai"},{"value":"TMA","text":"TMA - Theatre and Media Arts"},{"value":"TONGA","text":"TONGA - Tongan"},{"value":"TURK","text":"TURK - Turkish"},{"value":"UKRAI","text":"UKRAI - Ukrainian"},{"value":"UNIV","text":"UNIV - University Requirements"},{"value":"VIET","text":"VIET - Vietnamese"},{"value":"WELSH","text":"WELSH - Welsh"},{"value":"WRTG","text":"WRTG - Writing"},{"value":"WS","text":"WS - Global Women's Studies"},{"value":"WS","text":"WS - Women's Studies"}];

    var departmentSelect = new EditorWidgets.SuperSelect({
        el: "departmentLocation",
        id: "departments",
        value: [],
        icon: "icon-book",
        text: "Department",
        button: "left",
        multiple: false,
        options: $scope.departmentList,
        defaultValue: {value:"",text:"No Department Selected"}
    });

    var createCourseObj = function(d,c,s) {
        return {
            "department":d,
            "catalogNumber":c,
            "sectionNumber":s
        };
    };

    var pad = function(n) {
        if (typeof n === "number")
            n = "" + n;
        if (typeof n === "string") {
            n = n.replace("-","");
            var padded = ("000" + n).slice(-3);
            return padded !== "000" ? padded : null;
        } else
            return n;
    };


    $scope.appendCourse = function() {
        if (departmentSelect.value.length !== 1 || departmentSelect.value[0].trim().length === 0) {
            alert("Invalid department.");
            return;
        }
        var num = pad($scope.catalogNumber), sec = pad($scope.sectionNumber);
        var newCourse = createCourseObj(departmentSelect.value[0].trim(), num, sec);
        var courseAlreadyAdded = $scope.courses.some(function(c) {
            return c.department === newCourse.department &&
                c.catalogNumber === newCourse.catalogNumber &&
                c.sectionNumber === newCourse.sectionNumber;
        });
		if (!courseAlreadyAdded && !linkedCourses.contains(newCourse)) {
			$scope.courses.push(newCourse);
		} else {
			// course has already been added
            // TODO: alert user
        }
    };

    $scope.removeCourse = function(courseCode) {
        var index = $scope.courses.indexOf(courseCode);
        $scope.courses.splice(index, ~index?1:0);
    };

    $scope.submit = function() {
        $.ajax("/collection/" + $scope.$parent.collectionId + "/linkCourses", {
            type: "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify($scope.courses.map(function(c){return linkedCourses.omit(c, "$$hashKey")})),
            success: function(value) {
                // TODO: check for invalid json
                if (typeof value === "string") {
                    value = JSON.parse(value);
                } else if (!value instanceof Array) {
                    console.log("addCourseController [submit] - Unexpected response type.")
                    return;
                }
                $scope.courses = [];
                linkedCourses.append(value);
                $scope.$apply();
            },
            failure: function(err) {
                console.log(err);
            }
        });
    };
})
.controller("removeCourseController", function($scope, linkedCourses) {
    // $scope.courses defined with ng-init in the directive for this controller
    // in order to allow the back end to pass in the array of courses
    // need to wait for it to load using the watch function
    $scope.$watch("courses", function() {
        $scope.markedCourses = [];
        linkedCourses.set($scope.courses);
        // save a reference
        $scope.courses = linkedCourses.get();
    });

    $scope.toggleMarkCourse = function(course, $event) {
        if (!~$scope.markedCourses.indexOf(course)) {
            $scope.markedCourses.push(course);
            angular.element($event.currentTarget).addClass("list-group-item-danger");
        } else {
            $scope.markedCourses.splice($scope.markedCourses.indexOf(course), 1);
            angular.element($event.currentTarget).removeClass("list-group-item-danger");
        }
    };

    $scope.submit = function() {
        $.ajax("/collection/" + $scope.$parent.collectionId + "/unlinkCourses", {
            type: "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify($scope.markedCourses.map(function(c){return linkedCourses.omit(c, "$$hashKey")})),
            success: function(value) {
                // turn off spinner...
                // TODO: check for invalid json
                value = JSON.parse(value);
                if (value.rowsRemoved === 0) {
                    return;
                }
                $scope.courses = $scope.courses.filter(function(course) {
                    // remove the courses that are in markedCourses
                    return !~$scope.markedCourses.indexOf(course);
                });
                linkedCourses.remove($scope.markedCourses);
                $scope.markedCourses = [];
                $scope.$apply();
            },
            failure: function(err) {
                console.log(err.message);
            }
        });
    };
})

.controller("taController", function($scope) {
    $scope.taList = [];
    $scope.addTA = function(e) {
        var netid = document.getElementById("ta_netid").value;
        if (netid === ""){
            console.log("netid empty");
        }
        else {
            $.ajax("/collection/" + $scope.$parent.collectionId + "/addTA", {
                type: "post",
                cache: false,
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                data: JSON.stringify(netid),
                success: function(data) {
                    $scope.taList.push(data);
                    $scope.$apply();
                },
                error: function(data) {
                    console.log(data);
                    alert(data.responseText);
                }
            });
        }
    }
        
    $scope.removeTA = function(ta, $event){
        let button = $event.currentTarget;
        let table = button.parentNode.parentNode.parentNode;
        let row = button.parentNode.parentNode;

        // Add Bootstrap .danger class to current row
        row.classList.add("danger");

        setTimeout(function() {
          if (confirm(`Are you sure you want to remove \'${ta.name}\' from the Exceptions list?`)) {
            
            // Remove Exception on backend process
            $.ajax("/collection/" + $scope.$parent.collectionId + "/removeTA", {
                type: "post",
                dataType: "json",
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify(ta.username),
                success: function(value) {
                    console.log(value);
                    var index = $scope.taList.indexOf(ta);
                    console.log(index);
                    $scope.taList.splice(index, ~index?1:0);
                    console.log($scope.taList);
                    $scope.$apply();
                },
                error: function(xhr){
                    console.log("Error: " + JSON.parse(xhr.responseText)["Message"])
                }
            });
          }
          else {
            row.classList.remove("danger");
          }
        }, 100);
    };
})
.controller("addExceptionController", function($scope) {
    $scope.submit = function() {
        let studentId = document.getElementById("addStudentException").value
        $.ajax("/collection/" + $scope.$parent.collectionId + "/addException", {
            type: "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(studentId),
            success: function(value) {
                // console.log(value);

                let exceptionsTable = document.getElementById("exceptionsTable"),
                    exceptionsList = exceptionsTable.children[1]

                
                console.log(exceptionsList)
                debugger


                $scope.exceptions.push(value)
                $scope.$apply();
            },
            error: function(xhr){
                console.log("Error: " + JSON.parse(xhr.responseText)["Message"])
            }
        });
    };

    $scope.removeException = function(exception, $event){
        let button = $event.currentTarget;
        let table = button.parentNode.parentNode.parentNode;
        let row = button.parentNode.parentNode;

        // Add Bootstrap .danger class to current row
        row.classList.add("danger");

        setTimeout(function() {
          if (confirm(`Are you sure you want to remove \'${exception.name}\' from the Exceptions list?`)) {
            
            // Remove Exception on backend process
            $.ajax("/collection/" + $scope.$parent.collectionId + "/removeException", {
                type: "post",
                dataType: "json",
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify(exception.username),
                success: function(value) {
                    console.log("HI from removeException success!!");
                    console.log(value);
                    // $scope.exceptions.push(value)
                    // $scope.$apply();
                },
                error: function(xhr){
                    console.log("Error: " + JSON.parse(xhr.responseText)["Message"])
                }
            });

            var index = $scope.exceptions.indexOf(exception);
            console.log(index);
            $scope.exceptions.splice(index, ~index?1:0);
            console.log($scope.exceptions);
            $scope.$apply();

          }
          else {
            row.classList.remove("danger");
          }
        }, 100);
    };
});



