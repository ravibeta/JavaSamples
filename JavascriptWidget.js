(function() {
var jQuery;
// Initialize jQuery say as reference from http://alexmarandon.com/articles/web_widget_jquery/ and omitted here:

function main() { 
    jQuery(document).ready(function($) { 
        var jsonp_url = 'http://api.plos.org/search?q=title:%22Drosophila%22%20and%20body:%22RNA%22&fl=id,abstract&wt=json&indent=on&start=1&rows=1';
        $.getJSON(jsonp_url, function(data) {
          $('#example-widget-container').html("This data comes from another server: " + data.html);
        });
    });
}
})(); 

/*
Sample output:
{
  "response":{"numFound":1195,"start":1,"docs":[
      {
        "id":"10.1371/journal.pone.0188133",
        "abstract":["\nImmune challenges, such as parasitism, can be so pervasive and deleterious that they constitute an existential threat to a species’ survival. In response to these ecological pressures, organisms have developed a wide array of novel behavioral, cellular, and molecular adaptations. Research into these immune defenses in model systems has resulted in a revolutionary understanding of evolution and functional biology. As the field has expanded beyond the limited number of model organisms our appreciation of evolutionary innovation and unique biology has widened as well. With this in mind, we have surveyed the hemolymph of several non-model species of Drosophila. Here we identify and describe a novel hemocyte, type-II nematocytes, found in larval stages of numerous Drosophila species. Examined in detail in Drosophila falleni and Drosophila phalerata, we find that these remarkable cells are distinct from previously described hemocytes due to their anucleate state (lacking a nucleus) and unusual morphology. Type-II nematocytes are long, narrow cells with spindle-like projections extending from a cell body with high densities of mitochondria and microtubules, and exhibit the ability to synthesize proteins. These properties are unexpected for enucleated cells, and together with our additional characterization, we demonstrate that these type-II nematocytes represent a biological novelty. Surprisingly, despite the absence of a nucleus, we observe through live cell imaging that these cells remain motile with a highly dynamic cellular shape. Furthermore, these cells demonstrate the ability to form multicellular structures, which we suggest may be a component of the innate immune response to macro-parasites. In addition, live cell imaging points to a large nucleated hemocyte, type-I nematocyte, as the progenitor cell, leading to enucleation through a budding or asymmetrical division process rather than nuclear ejection: This study is the first to report such a process of enucleation. Here we describe these cells in detail for the first time and examine their evolutionary history in Drosophila.\n"]}]
  }}
  */
