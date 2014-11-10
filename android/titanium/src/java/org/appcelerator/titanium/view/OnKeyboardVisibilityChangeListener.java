/*
 * Copyright © 2014 Avego Ltd., All Rights Reserved.
 * For licensing terms please contact Avego LTD.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appcelerator.titanium.view;

/**
 * The OnKeyBoardVisibleListener represents
 * @version $Id$
 * @author wei.ding
 */
public interface OnKeyboardVisibilityChangeListener {

	/**
	 * This is executed when the keyboard is shown/hidden.
	 */
	void onKeyboardVisibilityChange(boolean visible);
}
