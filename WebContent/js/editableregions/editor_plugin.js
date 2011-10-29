/**
 * $Id: editor_plugin_src.js 743 2008-03-23 17:47:33Z spocke $
 *
 * @author Moxiecode
 * @copyright Copyright © 2004-2008, Moxiecode Systems AB, All rights reserved.
 * Modified by Andrew Green in 2008
 *
 * http://tinymce.moxiecode.com/punbb/viewtopic.php?id=14211
 */

(function() {
    var Event = tinymce.dom.Event;

    tinymce.create('tinymce.plugins.EditableRegionsPlugin', {
        init : function(ed, url) {
            var t = this, editClass, nonEditClass;

            t.editor = ed;
            editClass = ed.getParam("editableregions_editable_class", "mceEditable");
            nonEditClass = ed.getParam("editableregions_noneditable_class", "mceEditableRegions");

            ed.onInit.addToTop(function(ed, cm, n){
                ed.serializer.addRules("style[*],body[*],title[*],head[*],html[*]");
            });

            ed.onSetContent.addToTop(function(ed, cm, n){
                if (ed.getBody().contentEditable) {
                    ed.getBody().contentEditable = false;
                }else if(ed.getDoc().designMode){
                    ed.getDoc().designMode = '';
                }

                var mceEdit = t._getElementsByClassName('*',ed.getParam("editableregions_editable_class", "mceEditable"),ed.getWin());

                for(var i=0;i<mceEdit.length;i++){
                    mceEdit[i].contentEditable=true;

                }


            });
            ed.onNodeChange.addToTop(function(ed, cm, n) {
                var sc, ec;

                // Block if start or end is inside a non editable element
                sc = ed.dom.getParent(ed.selection.getStart(), function(n) {
                    return ed.dom.hasClass(n, editClass);
                });

                ec = ed.dom.getParent(ed.selection.getEnd(), function(n) {
                    return ed.dom.hasClass(n, editClass);
                });

                // Block or unblock
                if (!sc || !ec) {
                    t._setDisabled(1);
                    return false;
                } else
                    t._setDisabled(0);
            });
        },

        getInfo : function() {
            return {
                longname : 'editable regions elements',
                author : 'Moxiecode Systems AB(modified by Andrew Green)',
                authorurl : 'http://tinymce.moxiecode.com',
                infourl : 'http://wiki.moxiecode.com/index.php/TinyMCE:Plugins/editableregions',
                version : tinymce.majorVersion + "." + tinymce.minorVersion
            };
        },

        _block : function(ed, e) {
            var k = e.keyCode;

            // Don't block arrow keys, pg up/down, and F1-F12
            if ((k > 32 && k < 41) || (k > 111 && k < 124))
                return;

            return Event.cancel(e);
        },

        _setDisabled : function(s) {
            var t = this, ed = t.editor;



            tinymce.each(ed.controlManager.controls, function(c) {

                switch(c.settings.cmd){
                    case "mceFullScreen":
                    case "mceHelp":
                    case "mceSave":
                    case "mceCleanup":
                    case "mcePreview":
                    case "Undo":
                    case "Redo":
                    case "Redo":
                    case "":

                        break;
                    default:
                    switch(c.settings.title){
                        case "Exit":
                        case "Template":
                        case "Edit HTML Source":
                            break;
                        default:
                            c.setDisabled(s);
                    }
                }

            });

            if (s !== t.disabled) {
                if (s) {
                    ed.onKeyDown.addToTop(t._block);
                    ed.onKeyPress.addToTop(t._block);
                    ed.onKeyUp.addToTop(t._block);
                    ed.onPaste.addToTop(t._block);
                } else {
                    ed.onKeyDown.remove(t._block);
                    ed.onKeyPress.remove(t._block);
                    ed.onKeyUp.remove(t._block);
                    ed.onPaste.remove(t._block);
                }

                t.disabled = s;
            }
        },
        _getElementsByClassName : function(strTagName, strClassName,_Win)
        {
            if(!_Win)
                _Win=window;
            _Doc=_Win.document;
            var arrElements = (strTagName == "*" && _Doc.all)? _Doc.all : _Doc.getElementsByTagName(strTagName);
            var arrReturnElements = new Array();
            strClassName = strClassName.replace(/\-/g, "\\-");
            var oRegExp = new RegExp("(^|\\s)" + strClassName + "(\\s|$)");
            var oElement;
            for(var i=0; i<arrElements.length; i++){
                oElement = arrElements[i];
                if(oRegExp.test(oElement.className)){
                    arrReturnElements.push(oElement);
                }
            }
            return (arrReturnElements);

        }
    });

    // Register plugin
    tinymce.PluginManager.add('editableregions', tinymce.plugins.EditableRegionsPlugin);
})();

