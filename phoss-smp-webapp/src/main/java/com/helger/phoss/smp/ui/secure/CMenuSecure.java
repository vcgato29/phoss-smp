/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.ui.secure;

import javax.annotation.concurrent.Immutable;

/**
 * This class defines the menu item constants for the secure application
 *
 * @author Philip Helger
 */
@Immutable
public final class CMenuSecure
{
  // Menu item IDs
  public static final String MENU_USERS = "users";
  public static final String MENU_SERVICE_GROUPS = "service_groups";
  public static final String MENU_SERVICE_GROUPS_EXCHANGE = "service_groups_exchange";

  public static final String MENU_ENDPOINTS = "endpoints";
  public static final String MENU_ENDPOINTS_CHANGE_URL = "endpoints_change_url";
  public static final String MENU_ENDPOINTS_CHANGE_CERTIFICATE = "endpoints_change_certificate";

  public static final String MENU_REDIRECTS = "redirects";
  public static final String MENU_BUSINESS_CARDS = "business_cards";
  public static final String MENU_TRANSPORT_PROFILES = "transport_profiles";
  public static final String MENU_CERTIFICATE_INFORMATION = "certificate_information";
  public static final String MENU_TASKS = "tasks";

  public static final String MENU_ADMIN = "admin";
  public static final String MENU_SML = "sml";
  // Legacy name
  public static final String MENU_SML_CONFIGURATION = "sml_info";
  public static final String MENU_SML_REGISTRATION = "sml_registration";
  public static final String MENU_SML_CERTIFICATE_UPDATE = "sml_certificate_update";
  public static final String MENU_SMP_SETTINGS = "smp_settings";
  public static final String MENU_CHANGE_PASSWORD = "change_pw";

  private CMenuSecure ()
  {}
}
