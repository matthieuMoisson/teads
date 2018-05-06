package teads.model

case class AftModified (
                       etat: Int,
                       user_operating_system: String,
                       user_device: String,
                       cost: Float
                       )

/* system
Android -> 1
BlackBerry OS -> 2
Chrome OS -> 3
macOS -> 4
RIM OS -> 5
Fire OS -> 6
iOS -> 7
unknown -> 8
OS X -> 9
Linux -> 10
BSD -> 11
Windows -> 12
*/

/* device
PersonalComputer -> 1
Phone -> 2
Tablet -> 3
null -> 4
ConnectedTv -> 5
 */