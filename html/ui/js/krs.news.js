/******************************************************************************
 * Copyright © 2013-2016 The KPL Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * KPL software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {krs.js}
 */
var krs = (function(krs, $, undefined) {
	krs.pages.news = function() {
		if (krs.settings.news != 1) {
			$("#rss_news_container").hide();
			$("#rss_news_disabled").show();
			return;
		} else {
			$("#rss_news_container").show();
			$("#rss_news_disabled").hide();
		}

      $(".rss_news").empty().addClass("data-loading").html("<img src='img/loading_indicator.gif' width='32' height='32' />");
      var ssl = "";
      if (window.location.protocol == "https:") {
         ssl = "s";
      }
      var settings = {
         "limit": 5,
         "layoutTemplate": "<div class='list-group'>{entries}</div>",
         "entryTemplate": "<a href='{url}' target='_blank' class='list-group-item'><h4 class='list-group-item-heading'>{title}</h4><p class='list-group-item-text'>{shortBodyPlain}</p><i>{date}</i></a>",
         "ssl": ssl
      };

      var settingsReddit = {
         "limit": 7,
         "filterLimit": 5,
         "layoutTemplate": "<div class='list-group'>{entries}</div>",
         "entryTemplate": "<a href='{url}' target='_blank' class='list-group-item'><h4 class='list-group-item-heading'>{title}</h4><p class='list-group-item-text'>{shortBodyReddit}</p><i>{date}</i></a>",
         "tokens": {
            "shortBodyReddit": function(entry, tokens) {
               return entry.contentSnippet.replace("&lt;!-- SC_OFF --&gt;", "").replace("&lt;!-- SC_ON --&gt;", "").replace("[link]", "").replace("[comment]", "");
            }
         },
         "filter": function(entry, tokens) {
            return tokens.title.indexOf("Donations toward") == -1 && tokens.title.indexOf("KPL- tipping bot has arrived") == -1
         },
         "ssl": ssl
      };

      $("#KPLforum_news").rss("https://KPLforum.org/index.php?type=rss;action=.xml", settings, krs.newsLoaded);
      $("#reddit_news").rss("http://www.reddit.com/r/KPL/.rss", settingsReddit, krs.newsLoaded);
      $("#KPLcoin_blogspot_news").rss("http://KPLcoin.blogspot.com/feeds/posts/default", settings, krs.newsLoaded);
      $("#KPLer_news").rss("http://KPLer.org/feed/", settings, krs.newsLoaded);
		krs.pageLoaded();
	};

	krs.newsLoaded = function($el) {
		$el.removeClass("data-loading").find("img").remove();
	};

	$("#rss_news_enable").on("click", function() {
		krs.updateSettings("news", 1);
		krs.loadPage("news");
	});

	return krs;
}(krs || {}, jQuery));