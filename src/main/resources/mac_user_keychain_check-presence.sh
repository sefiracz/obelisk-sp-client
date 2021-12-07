#!/bin/bash
# © SEFIRA spol. s r.o., 2020-2021
#
# Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
# You may use this work only in accordance with the License.
# You can obtain a copy of the License at the following address:
#
# https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
#
# Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
# WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
# See the License for specific permissions and language restrictions under the License.

# Check whether certificate is already installed
security verify-cert -c "$1" -p ssl -L
exit $?