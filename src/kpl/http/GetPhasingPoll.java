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

import kpl.kplException;
import kpl.PhasingPoll;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPhasingPoll extends APIServlet.APIRequestHandler {

    static final GetPhasingPoll instance = new GetPhasingPoll();

    private GetPhasingPoll() {
        super(new APITag[]{APITag.PHASING}, "transaction", "countVotes");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws kplException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        boolean countVotes = "true".equalsIgnoreCase(req.getParameter("countVotes"));
        PhasingPoll phasingPoll = PhasingPoll.getPoll(transactionId);
        if (phasingPoll != null) {
            return JSONData.phasingPoll(phasingPoll, countVotes);
        }
        PhasingPoll.PhasingPollResult pollResult = PhasingPoll.getResult(transactionId);
        if (pollResult != null) {
            return JSONData.phasingPollResult(pollResult);
        }
        return JSONResponses.UNKNOWN_TRANSACTION;
    }
}