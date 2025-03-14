--
-- Copyright (C) 2015-2019 Philip Helger and contributors
-- philip[at]helger[dot]com
--
-- The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
--
-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at http://mozilla.org/MPL/2.0/.
--

USE `smp`;
-- add column
ALTER TABLE `smp_service_metadata_redirection` ADD COLUMN `certificate` LONGTEXT;
-- make column nullable
ALTER TABLE `smp_service_metadata_redirection` MODIFY `certificateUID` VARCHAR(256) NULL;
