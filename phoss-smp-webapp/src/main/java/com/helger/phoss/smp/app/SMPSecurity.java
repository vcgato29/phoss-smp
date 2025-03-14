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
package com.helger.phoss.smp.app;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.RoleManager;
import com.helger.photon.security.user.UserManager;
import com.helger.photon.security.usergroup.UserGroupManager;

/**
 * SMP security initialization code.
 * 
 * @author Philip Helger
 */
@Immutable
public final class SMPSecurity
{
  private SMPSecurity ()
  {}

  public static void init ()
  {
    final UserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final UserGroupManager aUserGroupMgr = PhotonSecurityManager.getUserGroupMgr ();
    final RoleManager aRoleMgr = PhotonSecurityManager.getRoleMgr ();

    // Standard users
    if (!aUserMgr.containsWithID (CSMP.USER_ADMINISTRATOR_ID))
    {
      final boolean bDisabled = false;
      aUserMgr.createPredefinedUser (CSMP.USER_ADMINISTRATOR_ID,
                                     CSMP.USER_ADMINISTRATOR_LOGINNAME,
                                     CSMP.USER_ADMINISTRATOR_EMAIL,
                                     CSMP.USER_ADMINISTRATOR_PASSWORD,
                                     CSMP.USER_ADMINISTRATOR_FIRSTNAME,
                                     CSMP.USER_ADMINISTRATOR_LASTNAME,
                                     CSMP.USER_ADMINISTRATOR_DESCRIPTION,
                                     CSMP.USER_ADMINISTRATOR_LOCALE,
                                     CSMP.USER_ADMINISTRATOR_CUSTOMATTRS,
                                     bDisabled);
    }

    // Create all roles
    if (!aRoleMgr.containsWithID (CSMP.ROLE_CONFIG_ID))
      aRoleMgr.createPredefinedRole (CSMP.ROLE_CONFIG_ID,
                                     CSMP.ROLE_CONFIG_NAME,
                                     CSMP.ROLE_CONFIG_DESCRIPTION,
                                     CSMP.ROLE_CONFIG_CUSTOMATTRS);
    if (!aRoleMgr.containsWithID (CSMP.ROLE_VIEW_ID))
      aRoleMgr.createPredefinedRole (CSMP.ROLE_VIEW_ID,
                                     CSMP.ROLE_VIEW_NAME,
                                     CSMP.ROLE_VIEW_DESCRIPTION,
                                     CSMP.ROLE_VIEW_CUSTOMATTRS);

    // User group Administrators
    if (!aUserGroupMgr.containsWithID (CSMP.USERGROUP_ADMINISTRATORS_ID))
    {
      aUserGroupMgr.createPredefinedUserGroup (CSMP.USERGROUP_ADMINISTRATORS_ID,
                                               CSMP.USERGROUP_ADMINISTRATORS_NAME,
                                               CSMP.USERGROUP_ADMINISTRATORS_DESCRIPTION,
                                               CSMP.USERGROUP_ADMINISTRATORS_CUSTOMATTRS);
      // Assign administrator user to administrators user group
      aUserGroupMgr.assignUserToUserGroup (CSMP.USERGROUP_ADMINISTRATORS_ID, CSMP.USER_ADMINISTRATOR_ID);
    }
    aUserGroupMgr.assignRoleToUserGroup (CSMP.USERGROUP_ADMINISTRATORS_ID, CSMP.ROLE_CONFIG_ID);
    aUserGroupMgr.assignRoleToUserGroup (CSMP.USERGROUP_ADMINISTRATORS_ID, CSMP.ROLE_VIEW_ID);

    // User group for Config users
    if (!aUserGroupMgr.containsWithID (CSMP.USERGROUP_CONFIG_ID))
      aUserGroupMgr.createPredefinedUserGroup (CSMP.USERGROUP_CONFIG_ID,
                                               CSMP.USERGROUP_CONFIG_NAME,
                                               CSMP.USERGROUP_CONFIG_DESCRIPTION,
                                               CSMP.USERGROUP_CONFIG_CUSTOMATTRS);
    aUserGroupMgr.assignRoleToUserGroup (CSMP.USERGROUP_CONFIG_ID, CSMP.ROLE_CONFIG_ID);

    // User group for View users
    if (!aUserGroupMgr.containsWithID (CSMP.USERGROUP_VIEW_ID))
      aUserGroupMgr.createPredefinedUserGroup (CSMP.USERGROUP_VIEW_ID,
                                               CSMP.USERGROUP_VIEW_NAME,
                                               CSMP.USERGROUP_VIEW_DESCRIPTION,
                                               CSMP.USERGROUP_VIEW_CUSTOMATTRS);
    aUserGroupMgr.assignRoleToUserGroup (CSMP.USERGROUP_VIEW_ID, CSMP.ROLE_VIEW_ID);

    // New login logs out old user
    LoggedInUserManager.getInstance ().setLogoutAlreadyLoggedInUser (true);

    // Setup internal error handler (if configured)
    SMPInternalErrorHandler.doSetup ();
  }
}
