/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017  Waltz open source project
 * See README.md for more information
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.web.endpoints.api;

import com.khartec.waltz.service.facet.FacetService;
import com.khartec.waltz.web.endpoints.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.web.WebUtilities.mkPath;
import static com.khartec.waltz.web.WebUtilities.readIdSelectionOptionsFromBody;
import static com.khartec.waltz.web.endpoints.EndpointUtilities.postForList;


@Service
public class FacetEndpoint implements Endpoint {

    private static final String BASE_URL = mkPath("api", "facet");

    private final FacetService facetService;


    @Autowired
    public FacetEndpoint(FacetService facetService) {
        checkNotNull(facetService, "facetService cannot be null");

        this.facetService = facetService;
    }


    @Override
    public void register() {
        String appKindTalliesPath = mkPath(BASE_URL, "count-by", "application", "kind");

        postForList(
                appKindTalliesPath,
                (req, res) -> facetService.getApplicationKindTallies(readIdSelectionOptionsFromBody(req)));
    }

}
