/******************************************************************************
 * Copyright © 2013-2016 The kpl Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * kpl software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package kpl.http;


import kpl.Kpl;
import kpl.kplException;
import kpl.Poll;
import kpl.db.DbIterator;
import kpl.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPolls extends APIServlet.APIRequestHandler {

    static final GetPolls instance = new GetPolls();

    private GetPolls() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.VS}, "account", "firstIndex", "lastIndex", "timestamp", "includeFinished", "finishedOnly");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws kplException {
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        boolean finishedOnly = "true".equalsIgnoreCase(req.getParameter("finishedOnly"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final int timestamp = ParameterParser.getTimestamp(req);

        JSONArray pollsJson = new JSONArray();
        DbIterator<Poll> polls = null;
        try {
            if (accountId == 0) {
                if (finishedOnly) {
                    polls = Poll.getPollsFinishingAtOrBefore(Kpl.getBlockchain().getHeight(), firstIndex, lastIndex);
                } else if (includeFinished) {
                    polls = Poll.getAllPolls(firstIndex, lastIndex);
                } else {
                    polls = Poll.getActivePolls(firstIndex, lastIndex);
                }
            } else {
                polls = Poll.getPollsByAccount(accountId, includeFinished, finishedOnly, firstIndex, lastIndex);
            }
            while (polls.hasNext()) {
                Poll poll = polls.next();
                if (poll.getTimestamp() < timestamp) {
                    break;
                }
                pollsJson.add(JSONData.poll(poll));
            }
        } finally {
            DbUtils.close(polls);
        }

        JSONObject response = new JSONObject();
        response.put("polls", pollsJson);
        return response;
    }
}
