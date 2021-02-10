var currentLocation;
var apiFilter = "allAPIs";
var statsEnabled = isDataPublishingEnabled();
currentLocation = window.location.pathname;

//setting default date
var to;
var from;

jagg.post("/site/blocks/stats/api-last-access-times/ajax/stats.jag", { action: "getFirstAccessTime", currentLocation: currentLocation  },
    function (json) {
        $('#spinner').hide();
        if (!json.error) {
            if (json.usage && json.usage.length > 0) {
                from = new Date(json.usage[0].year, json.usage[0].month - 1, json.usage[0].day);
                to = new Date();

                $("#apiFilter").change(function (e) {
                    apiFilter = this.value;
                    drawProviderAPIVersionUserLastAccess(from, to, apiFilter);
                });
                $('body').on('click', '.btn-group button', function (e) {
                    $(this).addClass('active');
                    $(this).siblings().removeClass('active');
                });
                drawProviderAPIVersionUserLastAccess(from, to, apiFilter);
            } else {
                $('.stat-page').html("");
                showEnableAnalyticsMsg();
            }
        } else {
            if (json.message == "AuthenticateError") {
                jagg.showLogin();
            } else {
                jagg.message({content: json.message, type: "error"});
            }
        }

    }, "json");


var drawProviderAPIVersionUserLastAccess = function() {
    jagg.post("/site/blocks/stats/api-last-access-times/ajax/stats.jag", { action:"getProviderAPIVersionUserLastAccess", currentLocation:currentLocation, apiFilter: apiFilter},
        function (json) {
            $('#spinner').hide();
            if (!json.error) {
                var length = json.usage.length;

                //getting timezone value
                var date=new Date();
                var offset = date.getTimezoneOffset();

                function convertToHHMM(info) {
                    var hrs = parseInt(Number(info));
                    var min = Math.round((Number(info)-hrs) * 60);
                    return (('' + hrs).length < 2 ? '0' : '') + hrs+':'+(('' + min).length < 2 ? '0' : '')+min;
                }

                var timezone;
                if(offset>=(-840) && offset<=720){
                    if(offset==0 || offset<0){
                        timezone=" (GMT+"+convertToHHMM(Math.abs(offset)/60)+")";
                    }
                    else{
                        timezone=" (GMT-"+ convertToHHMM(Math.abs(offset)/60)+")";
                    }
                }else{
                    timezone=" ";
                }

                $('#noData').empty();
                $('div#lastAccessTable_wrapper.dataTables_wrapper.no-footer').remove();

                var $dataTable =$('<table class="display table table-striped table-bordered" width="100%" cellspacing="0" id="lastAccessTable"></table>');

                $dataTable.append($('<thead class="tableHead"><tr>'+
                    '<th width="20%">API</th>'+
                    '<th width="15%">Version</th>'+
                    '<th width="15%">Subscriber</th>'+
                    '<th  style="text-align:right" width="30%">Access Time'+ timezone+'</th>'+
                    '</tr></thead>'));

                for (var i = 0; i < json.usage.length; i++) {
                    $dataTable.append($('<tr><td>' + json.usage[i].apiName + '</td><td>' + json.usage[i].apiVersion + '</td><td>' + json.usage[i].user + '</td><td class="tdNumberCell">' + formatTimeIn12HourFormat(new Date(Number(json.usage[i].lastAccessTime))) + '</td></tr>'));
                }
                if (length == 0) {
                    $('#lastAccessTable').hide();
                    $('div#lastAccessTable_wrapper.dataTables_wrapper.no-footer').remove();
                    $('#noData').html('');
                    $('#noData').append($('<div class="center-wrapper"><div class="col-sm-4"/><div class="col-sm-4 message message-info"><h4><i class="icon fw fw-info" title="No Stats"></i>'+i18n.t("No Data Available")+'</h4></div></div>'));

                }else{
                    $('#tableContainer').append($dataTable);
                    $('#tableContainer').show();
                    $('#lastAccessTable').datatables_extended({
                        "order": [[ 3, "desc" ]],
                        "fnDrawCallback": function(){
                            if(this.fnSettings().fnRecordsDisplay()<=$("#lastAccessTable_length option:selected" ).val()
                                || $("#lastAccessTable_length option:selected" ).val()==-1)
                                $('#lastAccessTable_paginate').hide();
                            else
                                $('#lastAccessTable_paginate').show();
                        },
                    });
                }
            } else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content:json.message,type:"error"});
                }
            }
        }, "json");

    /**
     * Format Time into MM/DD/YY hh:mm format
     * @param date
     * @returns {string}
     */
    function formatTimeIn12HourFormat(date) {
        var year = date.getFullYear();
        year = year < 2000 ? (year + 100) : year;
        var hours = date.getHours();
        var minutes = date.getMinutes();
        var ampm = hours >= 12 ? 'pm' : 'am';
        hours = hours % 12;
        hours = hours ? hours : 12; // the hour '0' should be '12'
        minutes = minutes < 10 ? '0' + minutes : minutes;
        var strTime = formatTimeChunk((date.getMonth() + 1)) + "/" + formatTimeChunk(date.getDate()) + "/"
            + year + ", " + hours + ':' + minutes + ' ' + ampm;
        return strTime;
    }
}
