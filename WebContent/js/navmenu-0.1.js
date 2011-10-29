/**
 *
 * this script manages the dynamic menu, each menu state is a digit in a base 4
 * encoded number:
 *   0: menu is open
 *   1: menu is opening
 *   2: menu is closed
 *   3: menu is closing
 *
 * dependencies:
 *  - jquery.js
 *  - jquery.cookie.js
 *
 */



/**
 *
 * set the menus to open/closed according to the cookie values
 * this also does some fancy animations for opening/closing
 * if menus are scheduled for opening/closing if a menu click
 * causes a page reload...
 */
jQuery(document).ready(function() {
   // name of the cookie used to store the menu state
   var COOKIE_NAME = "CHARMS_MENU";
   // store the cookie in the base path
   var COOKIE_PATH = "/";  // $('base').attr('href').replace(/^http:\/\/[^\/]*\//, '/').replace(/\/$/, '');
   // div container for the menu
   var MENU_CONTAINER_CLASS = "rich-page-sidebar";


   // get the cookie
   var cookie = jQuery.cookies.get(COOKIE_NAME);
   if (!cookie) {
     cookie = "";
   }
   // get the number of branches in the menu tree:
   //var branchesCount = $("." + MENU_CONTAINER_ID).find("ul").size();
   //// some checks to make sure the values are valid
   cookie.replace(/[^0-3]/g, "");

   var flags = [];
   flags = cookie.split("");

   // we need to make sure the first layer of the menu is visible
   if (flags.size == 0) {
	   flags.push("1"); // push opening flag for the base menu
   } else {
	   flags[0] = "0";
   }

   //alert("menu cookie/flags: " + cookie + "/" + flags );


   jQuery("div." + MENU_CONTAINER_CLASS).find("ul").each(
      function(i) {
        // i is a counter starting at 0
        // this is the actual ul element we are traversing

        //alert("flags.length: " + flags.length
        //+ " i: " + i
        //+ " flags: " + flags);

        // if we have no value for this li in the array we default to "1" and show
        if (flags.length <= i) {
            // hide all menus by default
            flags.push("2");  // 2 = closed
            jQuery(this).hide();
        } else {
          // calculate a dynamic delay time based on the number of menu items
          var delay = jQuery(this).children("li").size() * 100;
          switch(flags[i]) {
            case "0":                   // open
                jQuery(this).show();
                jQuery(this).prev("a").removeClass("closed").addClass('open');
                break;
            case "1":                   // opening
                jQuery(this).hide();
                jQuery(this).slideDown(delay);
                jQuery(this).prev("a").removeClass("closed").addClass('open');
                flags[i] = 0;
                break;
            case "2":                   // closed
                jQuery(this).hide();
                jQuery(this).prev("a").removeClass("open").addClass('closed');
                break;
            case "3":                   // closing
                jQuery(this).show();
                jQuery(this).slideUp(delay);
                jQuery(this).prev("a").removeClass("open").addClass('closed');
                flags[i] = 2;
                break;
            default:                  // unknown
                //alert("unknown state: " + flags[i]);
                jQuery(this).show();
                jQuery(this).prev("a").removeClass("closed").addClass('open');
                flags[i] = 0;
            }

        } // end if length too small


        //alert(" binding a click handler to: " + jQuery(this).prev("a").text());

        jQuery(this).prev("a").bind("click", function() {
            // variable i and flags are from the enclosing scope,
            //   seems this is like in scheme here
            // variable $(this) is the a tag

            //alert("clicked: " + i +
            //      " href: " + this.getAttribute('href') +
            //      "this: " + jQuery(this).next("ul").text() );


            // FIXME: check if the a tag has a valid link and set
            // flags[i] to 1/3 to open/close in the next page
            // maybe return true/false depending on link or not...

            var delay = jQuery(this).next("ul").children("li").size() * 100;
        	//var delay = 1000;
            // slideToggle should work here too
            //jQuery.blockUI({message: '<div class="throbber" />'});
            jQuery.blockUI();
            if (jQuery(this).next("ul").is(':visible')) {
                jQuery(this).next("ul").slideUp(delay, function() {
                	jQuery.unblockUI();
                    jQuery(this).removeClass("open").addClass('closed');
                });
                flags[i] = 2; // closed
            } else {
                jQuery(this).next("ul").slideDown(delay, function() {
                	jQuery.unblockUI();
                    jQuery(this).removeClass("closed").addClass('open');
                });
                flags[i] = 0;  // open
            }

            // store the new cookie
            jQuery.cookies.set(COOKIE_NAME,
                     flags.join(""), {
                              expires: 30, path: COOKIE_PATH
                     });
            //alert("cookie: " + flags.join(""));

            // create an overlay div on the fly and use it to prevent
            // further clicks before the page is reloaded
            //jQuery('BODY div.overlay').show(3000);
            //overlay.slideDown(5000);

            // unfocus the link
            jQuery(this).blur();

            // return false to prevent event bubbling, false also disables any link
            return true;
        });

      }
   );

   // set the cookie after initial animations are done
   jQuery.cookies.set(COOKIE_NAME, flags.join(""), { hoursToLive: 300, path: COOKIE_PATH } );

});
