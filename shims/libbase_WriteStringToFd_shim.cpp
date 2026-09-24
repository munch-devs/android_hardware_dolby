//
// SPDX-FileCopyrightText: Tashfin Shakeer Rhythm
// SPDX-License-Identifier: Apache-2.0
//

#include <android-base/file.h>
#include <string>
#include <string_view>

namespace android {
namespace base {

bool WriteStringToFd(const std::string &s, android::base::borrowed_fd fd) {
	return WriteStringToFd(std::string_view(s.data(), s.size()), fd);
}

} // namespace base
} // namespace android
