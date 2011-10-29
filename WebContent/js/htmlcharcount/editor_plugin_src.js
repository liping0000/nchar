/**
* $Id: editor_plugin_src.js 201 2007-02-12 15:56:56Z spocke $
*
* @author Chad Killingsworth, Missouri State University
*/

(function() {
    tinymce.create('tinymce.plugins.HTMLCharCount', {
        _MaxLength: 0,
        _CharsString: '',
        _RemainString: '',

        init: function(ed, url) {
            var t = this;
            t._MaxLength = ed.getParam('htmlcharcount_maxchars', 0);

            if(ed.getParam('theme', '') != 'advanced')
                return;

            t._CharsString = ' ' + ed.getLang('htmlcharcount.chars', 'Characters');
            t._RemainString = ' ' + ed.getLang('htmlcharcount.remaining', 'Characters remaining');


            ed.onPostRender.add(function(ed, cm) {
                 var PathTableRow = document.getElementById(ed.id + "_path_row").parentNode;
                tinymce.DOM.add(PathTableRow, 'div', { 'style': 'float: right', 'id': ed.id + '_charCounter' });
            });


            ed.onNodeChange.add(t._updateCount, t);
            ed.onKeyUp.add(t._updateCount, t);
        },

        _updateCount: function(ed, o) {
            document.getElementById(ed.id + '_charCounter').innerHTML = this._getPluginContent(ed);
        },
        _getPluginContent: function(ed) {
            var currCount = ed.getContent().length;
            if (this._MaxLength < 1)
                return currCount + this._CharsString;

            if (this._MaxLength > currCount)
                return (this._MaxLength - currCount) + this._RemainString;

            return "<span style='color: red'>" + (this._MaxLength - currCount) + this._RemainString + '</span>';
        },

        getInfo: function() {
            return {
                longname: 'HTML Character Counter plugin',
                author: 'Chad Killingsworth, Missouri State University',
                authorurl: 'http://www.missouristate.edu/web/',
                version: "1.0"
            };
        }
    });

    // Register plugin
    tinymce.PluginManager.add('htmlcharcount', tinymce.plugins.HTMLCharCount);
})();