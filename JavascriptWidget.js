(function() {
var jQuery;
// Initialize jQuery say as reference from http://alexmarandon.com/articles/web_widget_jquery/

function main() { 
    jQuery(document).ready(function($) { 
        var jsonp_url = 'http://api.plos.org/search?q=title:%22Drosophila%22%20and%20body:%22RNA%22&fl=id,abstract&wt=json&indent=on&start=1&rows=1';
        $.getJSON(jsonp_url, function(data) {
          $('#example-widget-container').html("This data comes from another server: " + data.html);
        });
    });
}
})(); 
