
// toggle helper

//
//  this extension is for the facets/tabs for request/handle messages etc.
//
//
;(function($){

  $.fn.openFormPanel = function(panelClass) {
    this.children('div:not(.' + panelClass + 'Pane)').hide(700);
    this.children('input.selectedPane').val(panelClass);
    this.children('div.' + panelClass + "Pane:hidden").show(700,
      function () {
        jQuery.scrollTo(jQuery('#footer'), 700);
      }
    );
    //alert("class is: " + panelClass);
    //jQuery.scrollTo(jQuery('div.' + panelClass + ":visible"));
    //jQuery.scrollTo(jQuery('#footer'));
  };

  $.fn.closeFormPanels = function() {
    this.children('div').hide(700);
    this.children('input.selectedPane').val('');
  };

})(jQuery);


//  extension for tabbing in the email template page
;(function($){

  $.fn.showTab = function(tabId) {
      $(tabId).siblings().hide();
      $(tabId).show();
      $('a[href="' + tabId + '"]').siblings().find('img').css('border','2px solid transparent');
      $('a[href="' + tabId + '"] img' ).css('border','2px solid white');
  }

})(jQuery);


/**
* @author Remy Sharp
* @url http://remysharp.com/2007/01/25/jquery-tutorial-text-box-hints/
*/

;(function($){

  $.fn.hint = function (blurClass) {
    if (!blurClass) {
      blurClass = 'blur';
    }

    return this.each(function () {
      // get the jQuery version of 'this'
      var input = $(this),
          title = input.attr('title'),
          form = $(this.form),
          win = $(window);

      function remove() {
        if (input.val() === title && input.hasClass(blurClass)) {
          input.val('').removeClass(blurClass);
        }
      }

      // only apply logic if the element has the attribute
      if (title) {
        // on blur, set value to title attr if text is blank
        input.blur(function () {
          if (this.value === '') {
            input.val(title).addClass(blurClass);
          }
        }).focus(remove).blur(); // now change all inputs to title

        // clear the pre-defined text when form is submitted
        form.submit(remove);
        win.unload(remove); // handles Firefox's autocomplete
      }
    });  // end foreach function
  };

})(jQuery);


;(function($){

  $.fn.zebra = function (linkTitle) {
    return this.each(function () {
      $(this).find('tbody tr:odd').addClass('odd');
      $(this).find('tbody tr:even').addClass('even');
    });
  };

})(jQuery);


;(function($){

  $.fn.equalHeight = function () {
    var height        = 0;
    var maxHeight     = 0;

    // Store the tallest element's height
    this.each(function () {
        //height        = jQuery(this).outerHeight();
    	height        = jQuery(this).height();
        maxHeight     = (height > maxHeight) ? height : maxHeight;
    });

    // Set element's min-height to tallest element's height
    return this.each(function () {
        var t            = jQuery(this);
        var minHeight    = maxHeight - (t.outerHeight() - t.height());
        var property     = jQuery.browser.msie && jQuery.browser.version < 7 ? 'height' : 'min-height';

        t.css(property, minHeight + 'px');
    });

  }

})(jQuery);


function popup(url) {
  var width  = 300;
  var height = 200;
  var left   = (screen.width  - width)/2;
  var top    = (screen.height - height)/2;
  var params = 'width='+width+', height='+height;
  params += ', top='+top+', left='+left;
  params += ', directories=no';
  params += ', location=no';
  params += ', menubar=no';
  params += ', resizable=no';
  params += ', scrollbars=no';
  params += ', status=no';
  params += ', toolbar=no';
  newwin=window.open(url,'windowname5', params);
  if (window.focus) {newwin.focus()};
  return false;
}


// overriding blockUI settings
jQuery.blockUI.defaults.css = {
  padding:	    0,
  margin:       0,
  width:		'30%',
  top:		    '40%',
  left:		    '35%',
  textAlign:	'center',
  color:		'#000',
  //border:		'3px solid #aaa',
  //backgroundColor:'#33f',
  cursor:		'wait'
};


jQuery.blockUI.defaults.overlayCSS = {
  backgroundColor: '#33f',
//  opacity:	  	   0.3,
  opacity:	  	   0.0,
  cursor:		   'wait'
};

jQuery.blockUI.defaults.applyPlatformOpacityRules = false;
//jQuery.blockUI.defaults.message = '<div class="throbber" />';
jQuery.blockUI.defaults.message = null;



jQuery.throbberUI   = function() { 
	jQuery.blockUI({
		message: '<div class="throbber" />',
		overlayCSS:  {
			backgroundColor: '#112',
			opacity:	  	 0.1,
			cursor:		  	 'wait'
		}
	}); 
};


