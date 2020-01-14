/*
 * Copyright 2019 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.media.provider

internal val SAMPLE_TRACKS = listOf(
    Track(161, "1741 (The Battle of Cartagena)", "Alestorm", "Sunset on the Golden Age", 437603, 1, 4, "", null, 1466283480, 26, 65, 17_506_481),
    Track(309, "The 2nd Law: Isolated System", "Muse", "The 2nd Law", 300042, 1, 13, "", null, 1439653800, 18, 40, 12_075_967),
    Track(481, "Dirty Water", "Foo Fighters", "Concrete and Gold", 320914, 1, 6, "", null, 1506374520, 13, 102, 12_912_282),
    Track(48, "Give It Up", "AC/DC", "Greatest Hits 30 Anniversary Edition", 233592, 1, 19, "", null, 1455310080, 5, 7, 5_716_578),
    Track(125, "Jailbreak", "AC/DC", "Greatest Hits 30 Anniversary Edition", 276668, 2, 14, "", null, 1455310140, 5, 7, 6_750_404),
    Track(294, "Knights of Cydonia", "Muse", "Black Holes and Revelations", 366946, 1, 11, "", null, 1414880700, 18, 38, 11_746_572),
    Track(219, "A Matter of Time", "Foo Fighters", "Wasting Light", 276140, 1, 8, "", null, 1360677660, 13, 26, 11_149_678),
    Track(75, "Nightmare", "Avenged Sevenfold", "Nightmare", 374648, 1, 1, "", null, 1439590380, 4, 6, 10_828_662),
    Track(464, "The Pretenders", "Foo Fighters", "Echoes, Silence, Patience & Grace", 266509, 1, 1, "", null, 1439653740, 13, 95, 4_296_041),
    Track(477, "Run", "Foo Fighters", "Concrete and Gold", 323424, 1, 2, "", null, 1506374520, 13, 102, 13_012_576)
)
internal val SAMPLE_ALBUMS = listOf(
    Album(40, "The 2nd Law", "Muse", 1, 2012, null, 18),
    Album(38, "Black Holes and Revelations", "Muse", 1, 2006, null, 18),
    Album(102, "Concrete and Gold", "Foo Fighters", 2, 2017, null, 13),
    Album(95, "Echoes, Silence, Patience & Grace", "Foo Fighters", 1, 2007, null, 13),
    Album(7, "Greatest Hits Anniversary Edition", "AC/DC", 2, 2010, null, 5),
    Album(6, "Nightmare", "Avenged Sevenfold", 1, 2010, null, 4),
    Album(65, "Sunset on the Golden Age", "Alestorm", 1, 2014, null, 26),
    Album(26, "Wasting Light", "Foo Fighters", 1, 2011, null, 13)
)

internal val SAMPLE_ARTISTS = listOf(
    Artist(5, "AC/DC", 1, 2, null),
    Artist(26, "Alestorm", 1, 1, null),
    Artist(4, "Avenged Sevenfold", 1, 1, null),
    Artist(13, "Foo Fighters", 4, 3, null),
    Artist(18, "Muse", 2, 2, null)
)