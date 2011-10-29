<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd"> <html lang="de" > <head> <script>function a(d){this.t={};this.tick=function(e,f,b){b=b?b:(new Date).getTime();this.t[e]=[b,f]};this.tick("start",null,d)}var c=new a;window.jstiming={Timer:a,load:c};try{var g=null;if(window.chrome&&window.chrome.csi)g=Math.floor(window.chrome.csi().pageT);if(g==null)if(window.gtbExternal)g=window.gtbExternal.pageT();if(g==null)if(window.external)g=window.external.pageT;if(g)window.jstiming.pt=g}catch(h){};
</script> <meta content="text/html; charset=utf-8" http-equiv="Content-Type" /> <base target="_top"> <title>Selenium IDE Flow Control</title> <style type="text/css">
/* default css */
table {
font-size: 1em;
line-height: inherit;
border-collapse: collapse;
}
tr {
text-align: left;
}
div, address, ol, ul, li, option, select {
margin-top: 0px;
margin-bottom: 0px;
}
p {
margin: 0px;
}
pre {
font-family: Courier New;
white-space: pre-wrap;
margin:0;
}
body {
margin: 6px;
padding: 0px;
font-family: Verdana, sans-serif;
font-size: 10pt;
background-color: #ffffff;
color: #000;
}
img {
-moz-force-broken-image-icon: 1;
}
@media screen {
html.pageview {
background-color: #f3f3f3 !important;
overflow-x: hidden;
overflow-y: scroll;
}
body {
min-height: 1100px;
counter-reset: __goog_page__;
}
* html body {
height: 1100px;
}
/* Prevent repaint errors when scrolling in Safari. This "Star-7" css hack
targets Safari 3.1, but not WebKit nightlies and presumably Safari 4.
That's OK because this bug is fixed in WebKit nightlies/Safari 4 :-). */
html*#wys_frame::before {
content: '\A0';
position: fixed;
overflow: hidden;
width: 0;
height: 0;
top: 0;
left: 0;
}
.pageview body {
border-top: 1px solid #ccc;
border-left: 1px solid #ccc;
border-right: 2px solid #bbb;
border-bottom: 2px solid #bbb;
width: 648px !important;
margin: 15px auto 25px;
padding: 40px 50px;
}
/* IE6 */
* html {
overflow-y: scroll;
}
* html.pageview body {
overflow-x: auto;
}
.writely-callout-data {
display: none;
}
.writely-footnote-marker {
background-image: url('images/footnote_doc_icon.gif');
background-color: transparent;
background-repeat: no-repeat;
width: 7px;
overflow: hidden;
height: 16px;
vertical-align: top;
-moz-user-select: none;
}
.editor .writely-footnote-marker {
cursor: move;
}
.writely-footnote-marker-highlight {
background-position: -15px 0;
-moz-user-select: text;
}
.writely-footnote-hide-selection ::-moz-selection, .writely-footnote-hide-selection::-moz-selection {
background: transparent;
}
.writely-footnote-hide-selection ::selection, .writely-footnote-hide-selection::selection {
background: transparent;
}
.writely-footnote-hide-selection {
cursor: move;
}
/* Comments */
.writely-comment-yellow {
background-color: #ffffd7;
}
.writely-comment-orange {
background-color: #ffe3c0;
}
.writely-comment-pink {
background-color: #ffd7ff;
}
.writely-comment-green {
background-color: #d7ffd7;
}
.writely-comment-blue {
background-color: #d7ffff;
}
.writely-comment-purple {
background-color: #eed7ff;
}
.br_fix span+br:not(:-moz-last-node) {
position:relative;
left: -1ex
}
#cb-p-tgt {
font-size: 8pt;
padding: .4em;
background-color: #ddd;
color: #333;
}
#cb-p-tgt-can {
text-decoration: underline;
color: #36c;
font-weight: bold;
margin-left: 2em;
}
#cb-p-tgt .spin {
width: 16px;
height: 16px;
background: url(//ssl.gstatic.com/docs/clipboard/spin_16o.gif) no-repeat;
}
}
h6 { font-size: 8pt }
h5 { font-size: 8pt }
h4 { font-size: 10pt }
h3 { font-size: 12pt }
h2 { font-size: 14pt }
h1 { font-size: 18pt }
blockquote {padding: 10px; border: 1px #DDD dashed }
.webkit-indent-blockquote { border: none; }
a img {border: 0}
.pb {
border-width: 0;
page-break-after: always;
/* We don't want this to be resizeable, so enforce a width and height
using !important */
height: 1px !important;
width: 100% !important;
}
.editor .pb {
border-top: 1px dashed #C0C0C0;
border-bottom: 1px dashed #C0C0C0;
}
div.google_header, div.google_footer {
position: relative;
margin-top: 1em;
margin-bottom: 1em;
}
/* Table of contents */
.editor div.writely-toc {
background-color: #f3f3f3;
border: 1px solid #ccc;
}
.writely-toc > ol {
padding-left: 3em;
font-weight: bold;
}
ol.writely-toc-subheading {
padding-left: 1em;
font-weight: normal;
}
/* IE6 only */
* html writely-toc ol {
list-style-position: inside;
}
.writely-toc-none {
list-style-type: none;
}
.writely-toc-decimal {
list-style-type: decimal;
}
.writely-toc-upper-alpha {
list-style-type: upper-alpha;
}
.writely-toc-lower-alpha {
list-style-type: lower-alpha;
}
.writely-toc-upper-roman {
list-style-type: upper-roman;
}
.writely-toc-lower-roman {
list-style-type: lower-roman;
}
.writely-toc-disc {
list-style-type: disc;
}
/* Ordered lists converted to numbered lists can preserve ordered types, and
vice versa. This is confusing, so disallow it */
ul[type="i"], ul[type="I"], ul[type="1"], ul[type="a"], ul[type="A"] {
list-style-type: disc;
}
ol[type="disc"], ol[type="circle"], ol[type="square"] {
list-style-type: decimal;
}
/* end default css */
/* custom css */
/* end custom css */
/* ui edited css */
body {
font-family: Verdana;
font-size: 10.0pt;
line-height: normal;
background-color: #ffffff;
}
/* end ui edited css */
/* editor CSS */
.editor a:visited {color: #551A8B}
.editor table.zeroBorder {border: 1px dotted gray}
.editor table.zeroBorder td {border: 1px dotted gray}
.editor table.zeroBorder th {border: 1px dotted gray}
.editor div.google_header, .editor div.google_footer {
border: 2px #DDDDDD dashed;
position: static;
width: 100%;
min-height: 2em;
}
.editor .misspell {background-color: yellow}
.editor .writely-comment {
font-size: 9pt;
line-height: 1.4;
padding: 1px;
border: 1px dashed #C0C0C0
}
/* end editor CSS */
</style> <style>
body {
margin: 0px;
}
#doc-contents {
margin: 6px;
}
#google-view-footer {
clear: both;
border-top: thin solid;
padding-top: 0.3em;
padding-bottom: 0.3em;
}
a.google-small-link:link, a.google-small-link:visited {
color:#112ABB;
font-family:Arial,Sans-serif;
font-size:11px !important;
}
body, p, div, td {
direction: inherit;
}
@media print {
#google-view-footer {
display: none;
}
}
</style> <script>
function viewOnLoad() {
if (document.location.href.indexOf('spi=1') != -1) {
if (navigator.userAgent.toLowerCase().indexOf('msie') != -1) {
window.print();
} else {
window.setTimeout(window.print, 10);
}
}
if (document.location.href.indexOf('hgd=1') != -1) {
var footer = document.getElementById("google-view-footer");
if (footer) {
footer.style.display = 'none';
}
}
}
</script> </head> <body onload="window.jstiming.load.tick('ol'); window.jstiming.report(window.jstiming.load, null, document.location.protocol == 'https:' ? 'https://gg.google.com/csi' : null);"> <div id="doc-contents"> var gotoLabels= {};<br id="bqc-0">var whileLabels = {}; <br id="bqc-1"><br id="bqc-2">// overload the original Selenium reset function<br id="bqc-3">Selenium.prototype.reset = function() {<br id="bqc-4">&nbsp;&nbsp;&nbsp; // reset the labels<br id="bqc-5">&nbsp;&nbsp;&nbsp; this.initialiseLabels();<br id="bqc-6"> &nbsp;&nbsp;&nbsp; // proceed with original reset code<br id="bqc-7">&nbsp;&nbsp;&nbsp; this.defaultTimeout = Selenium.DEFAULT_TIMEOUT; <br id="bqc-8">&nbsp;&nbsp;&nbsp; this.browserbot.selectWindow(&quot;null&quot;); <br id="bqc-9">&nbsp;&nbsp;&nbsp; this.browserbot.resetPopups();<br id="bqc-10">}<br id="bqc-11"><br id="bqc-12">Selenium.prototype.initialiseLabels = function()<br id="bqc-13">{<br id="bqc-14">&nbsp;&nbsp;&nbsp; gotoLabels = {};<br id="bqc-15">&nbsp;&nbsp;&nbsp; whileLabels = { ends: {}, whiles: {} };<br id="bqc-16">&nbsp;&nbsp;&nbsp; var command_rows = [];<br id="bqc-17">&nbsp;&nbsp;&nbsp; var numCommands = testCase.commands.length;<br id="bqc-18">&nbsp;&nbsp;&nbsp; for (var i = 0; i &lt; numCommands; ++i) {<br id="bqc-19">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; var x = testCase.commands[i];<br id="bqc-20">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; command_rows.push(x);<br id="bqc-21">&nbsp;&nbsp;&nbsp; } <br id="bqc-22">&nbsp;&nbsp;&nbsp; var cycles = [];<br id="bqc-23">&nbsp;&nbsp;&nbsp; for( var i = 0; i &lt; command_rows.length; i++ ) {<br id="bqc-24">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; if (command_rows[i].type == &#39;command&#39;)<br id="bqc-25">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; switch( command_rows[i].command.toLowerCase() ) {<br id="bqc-26">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; case &quot;label&quot;:<br id="bqc-27">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; gotoLabels[ command_rows[i].target ] = i;<br id="bqc-28">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; break;<br id="bqc-29">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; case &quot;while&quot;:<br id="bqc-30">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; case &quot;endwhile&quot;:<br id="bqc-31">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; cycles.push( [command_rows[i].command.toLowerCase(), i] )<br id="bqc-32">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; break; <br id="bqc-33">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; }<br id="bqc-34">&nbsp;&nbsp;&nbsp; }&nbsp;&nbsp; <br id="bqc-35">&nbsp;&nbsp;&nbsp; var i = 0; <br id="bqc-36">&nbsp;&nbsp;&nbsp; while( cycles.length ) {<br id="bqc-37">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; if( i &gt;= cycles.length ) {<br id="bqc-38">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp; throw new Error( &quot;non-matching while/endWhile found&quot; );<br id="bqc-39">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;}<br id="bqc-40">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;switch( cycles[i][0] ) {<br id="bqc-41">&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;case &quot;while&quot;:<br id="bqc-42"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; if( ( i+1 &lt; cycles.length ) &amp;&amp; ( &quot;endwhile&quot; == cycles[i+1][0] ) ) {<br id="bqc-43"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; // pair found<br id="bqc-44"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;whileLabels.ends[ cycles[i+1][1] ] = cycles[i][1];<br id="bqc-45"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;whileLabels.whiles[ cycles[i][1] ] = cycles[i+1][1];<br id="bqc-46"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;cycles.splice( i, 2 );<br id="bqc-47"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;i = 0;<br id="bqc-48"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;} else ++i;<br id="bqc-49"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;break;<br id="bqc-50"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;case &quot;endwhile&quot;:<br id="bqc-51"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;++i;<br id="bqc-52"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;break;<br id="bqc-53">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; }<br id="bqc-54">&nbsp;&nbsp;&nbsp; } <br id="bqc-55">} <br id="bqc-56"><br id="bqc-57">Selenium.prototype.continueFromRow = function( row_num ) <br id="bqc-58">{<br id="bqc-59">&nbsp;&nbsp;&nbsp; if(row_num == undefined || row_num == null || row_num &lt; 0) {<br id="bqc-60"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; throw new Error( &quot;Invalid row_num specified.&quot; );<br id="bqc-61"> &nbsp;&nbsp;&nbsp; }<br id="bqc-62"> &nbsp;&nbsp;&nbsp; testCase.debugContext.debugIndex = row_num;<br id="bqc-63">}<br id="bqc-64"><br id="bqc-65">// do nothing. simple label<br id="bqc-66">Selenium.prototype.doLabel = function(){};<br id="bqc-67"><br id="bqc-68">Selenium.prototype.doGotolabel = function( label ) <br id="bqc-69">{<br id="bqc-70">&nbsp;&nbsp;&nbsp; if( undefined == gotoLabels[label] ) {<br id="bqc-71"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; throw new Error( &quot;Specified label &#39;&quot; + label + &quot;&#39; is not found.&quot; );<br id="bqc-72"> &nbsp;&nbsp;&nbsp; }<br id="bqc-73"> &nbsp;&nbsp;&nbsp; this.continueFromRow( gotoLabels[ label ] );<br id="bqc-74">};<br id="bqc-75"><br id="bqc-76">Selenium.prototype.doGoto = Selenium.prototype.doGotolabel;<br id="bqc-77"><br id="bqc-78">Selenium.prototype.doGotoIf = function( condition, label ) <br id="bqc-79">{<br id="bqc-80">&nbsp;&nbsp;&nbsp; if( eval(condition) ) this.doGotolabel( label );<br id="bqc-81">}<br id="bqc-82"><br id="bqc-83">Selenium.prototype.doWhile = function( condition ) <br id="bqc-84">{<br id="bqc-85">&nbsp;&nbsp;&nbsp; if( !eval(condition) ) {<br id="bqc-86"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; var last_row = testCase.debugContext.debugIndex;<br id="bqc-87"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; var end_while_row = whileLabels.whiles[ last_row ];<br id="bqc-88"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; if( undefined == end_while_row ) throw new Error( &quot;Corresponding &#39;endWhile&#39; is not found.&quot; );<br id="bqc-89"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; this.continueFromRow( end_while_row );<br id="bqc-90"> &nbsp;&nbsp;&nbsp; }<br id="bqc-91">}<br id="bqc-92"><br id="bqc-93">Selenium.prototype.doEndWhile = function() <br id="bqc-94">{<br id="bqc-95">&nbsp;&nbsp;&nbsp; var last_row = testCase.debugContext.debugIndex;<br id="bqc-96"> &nbsp;&nbsp;&nbsp; var while_row = whileLabels.ends[ last_row ] - 1;<br id="bqc-97"> &nbsp;&nbsp;&nbsp; if( undefined == while_row ) throw new Error( &quot;Corresponding &#39;While&#39; is not found.&quot; );<br id="bqc-98"> &nbsp;&nbsp;&nbsp; this.continueFromRow( while_row );<br id="bqc-99">}<br> <br clear="all" /> </div> <div id="google-view-footer"> <div id="maybecanedit" style="float:right"><a class="google-small-link" id="editpermissionlink" href="Doc?tab=edit&dr=true&id=dm6sk55_37fmpg8tcr" title="Diese Seite bearbeiten"> Diese Seite bearbeiten (sofern Sie über die erforderlichen Rechte verfügen)</a> <span style="color:#676767;">|</span> <input id="report-abuse-button" type="button" value="Missbrauch melden" onclick="reportAbuse();"></div> <div style="float:left"> <a title="Weitere Informationen zu Google Text &amp; Tabellen" class="google-small-link" href="/"> Google Text &amp; Tabellen – Textverarbeitung, Präsentationen und Tabellen im Web.</a> </div> <p> &nbsp; </div> <script><!--
    viewOnLoad();
    if(window.jstiming){window.jstiming.a={};window.jstiming.c=1;var j=function(b,c,e){var a=b.t[c],g=b.t.start;if(a&&(g||e)){a=b.t[c][0];g=e!=undefined?e:g[0];return a-g}},n=function(b,c,e){var a="";if(window.jstiming.pt){a+="&srt="+window.jstiming.pt;delete window.jstiming.pt}try{if(window.external&&window.external.tran)a+="&tran="+window.external.tran;else if(window.gtbExternal&&window.gtbExternal.tran)a+="&tran="+window.gtbExternal.tran();else if(window.chrome&&window.chrome.csi)a+="&tran="+window.chrome.csi().tran}catch(g){}var d=
window.chrome;if(d)if(d=d.loadTimes){if(d().wasFetchedViaSpdy)a+="&p=s";if(d().wasNpnNegotiated)a+="&npn=1";if(d().wasAlternateProtocolAvailable)a+="&apa=1"}if(b.b)a+="&"+b.b;d=b.t;var m=d.start,k=[],h=[],f;for(f in d)if(f!="start")if(f.indexOf("_")!=0){var i=d[f][1];if(i)d[i]&&h.push(f+"."+j(b,f,d[i][0]));else m&&k.push(f+"."+j(b,f))}delete d.start;if(c)for(var l in c)a+="&"+l+"="+c[l];return[e?e:"http://csi.gstatic.com/csi","?v=3","&s="+(window.jstiming.sn||"writely")+"&action=",b.name,h.length?
"&it="+h.join(","):"","",a,"&rt=",k.join(",")].join("")};window.jstiming.report=function(b,c,e){b=n(b,c,e);if(!b)return"";c=new Image;var a=window.jstiming.c++;window.jstiming.a[a]=c;c.onload=c.onerror=function(){delete window.jstiming.a[a]};c.src=b;c=null;return b}};

    window.jstiming.load.name = 'published';
    
    
    var urchinPage = "/View";

    
    function getXHR() {
      if (typeof XMLHttpRequest != "undefined") {
        return new XMLHttpRequest();
      }
      try { return new ActiveXObject("Msxml2.XMLHTTP.6.0") } catch(e) {}
      try { return new ActiveXObject("Msxml2.XMLHTTP.3.0") } catch(e) {}
      try { return new ActiveXObject("Msxml2.XMLHTTP") } catch(e) {}
      try { return new ActiveXObject("Microsoft.XMLHTTP") } catch(e) {}
      return null;
    }

    function reportAbuse() {
      var req = getXHR();
      if (req) {
        
          var docid = 'dm6sk55_37fmpg8tcr';
          var posttoken = '';
        
        req.onreadystatechange = function() {
          try {
            if (req.readyState == 4 && req.status == 200) {
              var button = document.getElementById("report-abuse-button");
              button.value = 'Vielen Dank!';
              button.disabled = true;
            }
          } catch (ex) {
            
          }
        }
        try {
          req.open('POST', 'MiscCommands', true);
          req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
          req.send('command=report_abuse&abuseDoc=' + encodeURIComponent(docid) +
                   '&POST_TOKEN=' + encodeURIComponent(posttoken));
        } catch (ex) {
          
        }
      }
    }
  --></script> </body> </html> 