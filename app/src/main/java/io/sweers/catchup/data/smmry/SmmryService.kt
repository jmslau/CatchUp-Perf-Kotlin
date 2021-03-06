/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.smmry

import io.reactivex.Single
import io.sweers.catchup.data.smmry.model.SmmryResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface SmmryService {

  @POST(".") fun summarizeUrl(
      @QueryMap params: Map<String, @JvmSuppressWildcards Any>): Single<SmmryResponse>

  @POST(".") @FormUrlEncoded fun summarizeText(
      @QueryMap params: Map<String, @JvmSuppressWildcards Any>,
      @Field("sm_api_input") text: String): Single<SmmryResponse>

  companion object {

    val HOST = "api.smmry.com/"
    val ENDPOINT = "http://" + HOST
  }
}
