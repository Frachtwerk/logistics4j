/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of logistics4j.
 *
 * logistics4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logistics4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with logistics4j. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Wechselbruecke {
  @JacksonXmlProperty(localName = "WAB_WB01")
  private String wechselbruecke1;

  @JacksonXmlProperty(localName = "WAB_WB02")
  private String wechselbruecke2;

  public String getWechselbruecke1() {
    return wechselbruecke1;
  }

  public void setWechselbruecke1(String wechselbruecke1) {
    this.wechselbruecke1 = wechselbruecke1;
  }

  public String getWechselbruecke2() {
    return wechselbruecke2;
  }

  public void setWechselbruecke2(String wechselbruecke2) {
    this.wechselbruecke2 = wechselbruecke2;
  }
}
