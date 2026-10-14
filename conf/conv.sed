# conv.sed - converts Shimeji EE configuration files to the standard Japanese format
#
# Usage : Execute in the directory containing the script and all 4 configuration files :
# sed -f conv.sed actions.xml > Actions.xml
# sed -f conv.sed behaviors > Behavior.xml
#
# ------------------
# IMPORTANT : Make sure you use a Japanese 'Mascot.xsd' schema file with your converted
# config files, do not replace it with an English one
# ------------------

s/<Mascot /<マスコット /g
s/<ActionList>/<動作リスト>/g
s/<Action /<動作 /g
s/Name=/名前=/g
s/\"Look\"/\"振り向く\"/g
s/\"Embedded\"/\"組み込み\"/g
s/Class=/クラス=/g
s/\"Offset\"/\"変位\"/g
s/<!-- Standing -->/<!-- Stehen -->/g
s/\"Stand\"/\"立つ\"/g
s/\"Stay\"/\"静止\"/g
s/BorderType=/枠=/g
s/Type=/種類=/g
s/\"Floor\"/\"地面\"/g
s/<Animation>/<アニメーション>/g
s/<Pose /<ポーズ /g
s/Image=/画像=/g
s/ImageAnchor=/基準座標=/g
s/Velocity=/移動速度=/g
s/Duration=/長さ=/g
s/<\/Animation>/<\/アニメーション>/g
s/<\/Action>/<\/動作>/g
s/\"Walk\"/\"歩く\"/g
s/\"Move\"/\"移動\"/g
s/\"Run\"/\"走る\"/g
s/\"Dash\"/\"猛ダッシュ\"/g
s/<!-- Sitting -->/<!-- Sitzen -->/g
s/\"Sit\"/\"座る\"/g
s/\"SitAndLookUp\"/\"座って見上げる\"/g
s/\"SitAndLookAtMouse\"/\"座ってマウスを見上げる\"/g
s/<Animation /<アニメーション /g
s/Condition=/条件=/g
s/<\/Animation>/<\/アニメーション>/g
s/\"SitAndSpinHeadAction\"/\"座って首が回る\"/g
s/\"Animate\"/\"固定\"/g
s/\"SitWithLegsUp\"/\"楽に座る\"/g
s/\"SitWithLegsDown\"/\"足を下ろして座る\"/g
s/\"SitAndDangleLegs\"/\"足をぶらぶらさせる\"/g
s/<!-- Laying -->/<!-- Liegen -->/g
s/\"Sprawl\"/\"寝そべる\"/g
s/\"Creep\"/\"ずりずり\"/g
s/<!-- Ceiling -->/<!-- Decke -->/g
s/\"GrabCeiling\"/\"天井に掴まる\"/g
s/\"Ceiling\"/\"天井\"/g
s/\"ClimbCeiling\"/\"天井を伝う\"/g
s/<!-- Wall -->/<!-- Wand -->/g
s/\"GrabWall\"/\"壁に掴まる\"/g
s/\"Wall\"/\"壁\"/g
s/\"ClimbWall\"/\"壁を登る\"/g
s/TargetY/目的地Y/g
s/TargetX/目的地X/g
s/<!-- IE -->/<!-- IE-Fenster -->/g
s/\"FallWithIe\"/\"IEを持って落ちる\"/g
s/IeOffsetX=/IEの端X=/g
s/IeOffsetY=/IEの端Y=/g
s/\"WalkWithIe\"/\"IEを持って歩く\"/g
s/\"RunWithIe\"/\"IEを持って走る\"/g
s/\"ThrowIe\"/\"IEを投げる\"/g
s/InitialVX=/初速X=/g
s/InitialVY=/初速Y=/g
s/Gravity=/重力=/g
s/<!-- Falling -->/<!-- Fallen -->/g
s/\"Jumping\"/\"ジャンプ\"/g
s/VelocityParam=/速度=/g
s/\"Falling\"/\"落ちる\"/g
s/RegistanceX=/空気抵抗X=/g
s/RegistanceY=/空気抵抗Y=/g
s/\"Bouncing\"/\"跳ねる\"/g
s/\"Tripping\"/\"転ぶ\"/g
s/<!-- Dragging -->/<!-- Ziehen mit der Maus -->/g
s/\"Pinched\"/\"つままれる\"/g
s/\"Resisting\"/\"抵抗する\"/g
s/<\/ActionList>/<\/動作リスト>/g
s/<!-- Actual Behavior -->/<!-- Ausgeführte Verhaltensweisen -->/g
s/<!-- ALWAYS REQUIRED -->/<!-- Vom System benötigt -->/g
s/\"Fall\"/\"落下する\"/g
s/\"Sequence\"/\"複合\"/g
s/Loop=/繰り返し=/g
s/<ActionReference /<動作参照 /g
s/\"Select\"/\"選択\"/g
s/\"Dragged\"/\"ドラッグされる\"/g
s/\"Thrown\"/\"投げられる\"/g
s/\"StandUp\"/\"立ってボーっとする\"/g
s/\"SitDown\"/\"座ってボーっとする\"/g
s/\"LieDown\"/\"寝そべってボーっとする\"/g
s/\"SitWhileDanglingLegs\"/\"座って足をぶらぶらさせる\"/g
s/\"HoldOntoWall\"/\"壁に掴まってボーっとする\"/g
s/\"FallFromWall\"/\"壁から落ちる\"/g
s/\"HoldOntoCeiling\"/\"天井に掴まってボーっとする\"/g
s/\"FallFromCeiling\"/\"天井から落ちる\"/g
s/\"WalkAlongWorkAreaFloor\"/\"ワークエリアの下辺を歩く\"/g
s/\"RunAlongWorkAreaFloor\"/\"ワークエリアの下辺を走る\"/g
s/\"CrawlAlongWorkAreaFloor\"/\"ワークエリアの下辺でずりずり\"/g
s/\"WalkLeftAlongFloorAndSit\"/\"ワークエリアの下辺の左の端っこで座る\"/g
s/\"WalkRightAlongFloorAndSit\"/\"ワークエリアの下辺の右の端っこで座る\"/g
s/LookRight=/右向き=/g
s/\"GrabWorkAreaBottomLeftWall\"/\"ワークエリアの下辺から左の壁によじのぼる\"/g
s/\"GrabWorkAreaBottomRightWall\"/\"ワークエリアの下辺から右の壁によじのぼる\"/g
s/\"WalkLeftAndSit\"/\"走ってワークエリアの下辺の左の端っこで座る\"/g
s/\"WalkRightAndSit\"/\"走ってワークエリアの下辺の右の端っこで座る\"/g
s/\"WalkAndGrabBottomLeftWall\"/\"走ってワークエリアの下辺から左の壁によじのぼる\"/g
s/\"WalkAndGrabBottomRightWall\"/\"走ってワークエリアの下辺から右の壁によじのぼる\"/g
s/\"JumpFromBottomOfIE\"/\"IEの下に飛びつく\"/g
s/\"ClimbHalfwayAlongWall\"/\"ワークエリアの壁を途中まで登る\"/g
s/<BehaviorList>/<行動リスト>/g
s/<Behavior /<行動 /g
s/Frequency=/頻度=/g
s/<NextBehavior /<次の行動リスト /g
s/Add=/追加=/g
s/<\/NextBehavior>/<\/次の行動リスト>/g
s/<\/Behavior>/<\/行動>/g
s/\"ChaseMouse\"/\"マウスの周りに集まる\"/g
s/<BehaviorReference /<行動参照 /g
s/\"SitAndFaceMouse\"/\"座ってマウスのほうを見る\"/g
s/\"SitAndSpinHead\"/\"座ってマウスのほうを見てたら首が回った\"/g
s/\"PullUp\"/\"引っこ抜かれる\"/g
s/\"Divided\"/\"分裂した\"/g
s/<!-- On the Floor -->/<!-- Bei Bodenkontakt -->/g
s/<Condition /<条件 /g
s/\"SplitIntoTwo\"/\"分裂する\"/g
s/<\/Condition>/<\/条件>/g
s/<!-- On the Wall -->/<!-- Bei Wandkontakt -->/g
s/<!-- On the Ceiling -->/<!-- Bei Deckenkontakt -->/g
s/<!-- On Work Area Floor -->/<!-- Am unteren Rand des Arbeitsbereichs -->/g
s/<!-- Finished Crawling -->/<!-- Nach dem Kriechen untätig bleiben -->/g
s/\"PullUpShimeji\"/\"引っこ抜く\"/g
s/<!-- On Work Area Facing the Wall -->/<!-- An der Seitenwand des Arbeitsbereichs -->/g
s/\"ClimbAlongWall\"/\"ワークエリアの壁を登る\"/g
s/<!-- On Work Area Top Facing -->/<!-- Am oberen Rand des Arbeitsbereichs -->/g
s/\"ClimbAlongCeiling\"/\"ワークエリアの上辺を伝う\"/g
s/<!-- On Top of IE -->/<!-- Am oberen Rand des IE-Fensters -->/g
s/\"WalkAlongIECeiling\"/\"IEの天井を歩く\"/g
s/\"RunAlongIECeiling\"/\"IEの天井を走る\"/g
s/\"CrawlAlongIECeiling\"/\"IEの天井でずりずり\"/g
s/<!-- Finished Crawling -->/<!-- Nach dem Kriechen untätig bleiben -->/g
s/\"SitOnTheLeftEdgeOfIE\"/\"IEの天井の左の端っこで座る\"/g
s/\"SitOnTheRightEdgeOfIE\"/\"IEの天井の右の端っこで座る\"/g
s/\"JumpFromLeftEdgeOfIE\"/\"IEの天井の左の端っこから飛び降りる\"/g
s/\"JumpFromRightEdgeOfIE\"/\"IEの天井の右の端っこから飛び降りる\"/g
s/\"WalkLeftAlongIEAndSit\"/\"走ってIEの天井の左の端っこで座る\"/g
s/\"WalkRightAlongIEAndSit\"/\"走ってIEの天井の右の端っこで座る\"/g
s/\"WalkLeftAlongIEAndJump\"/\"走ってIEの天井の左の端っこから飛び降りる\"/g
s/\"WalkRightAlongIEAndJump\"/\"走ってIEの天井の右の端っこから飛び降りる\"/g
s/<!-- On IE`s Side -->/<!-- An der Seitenwand des IE-Fensters -->/g
s/\"HoldOntoIEWall\"/\"IEの壁を途中まで登る\"/g
s/\"ClimbIEWall\"/\"IEの壁を登る\"/g
s/<!-- On the Bottom of IE -->/<!-- Am unteren Rand des IE-Fensters -->/g
s/\"ClimbIEBottom\"/\"IEの下辺を伝う\"/g
s/\"GrabIEBottomLeftWall\"/\"IEの下辺から左の壁によじのぼる\"/g
s/\"GrabIEBottomRightWall\"/\"IEの下辺から右の壁によじのぼる\"/g
s/\"JumpFromLeftWall\"/\"左の壁に飛びつく\"/g
s/\"JumpFromRightWall\"/\"右の壁に飛びつく\"/g
s/<!-- IE Is Visible -->/<!-- Wenn das IE-Fenster sichtbar ist -->/g
s/\"JumpOnIELeftWall\"/\"IEの左に飛びつく\"/g
s/\"JumpOnIERightWall\"/\"IEの右に飛びつく\"/g
s/\"ThrowIEFromLeft\"/\"IEを右に投げる\"/g
s/\"ThrowIEFromRight\"/\"IEを左に投げる\"/g
s/\"WalkAndThrowIEFromRight\"/\"走ってIEを右に投げる\"/g
s/\"WalkAndThrowIEFromLeft\"/\"走ってIEを左に投げる\"/g
s/<\/BehaviorList>/<\/行動リスト>/g
s/<\/Mascot>/<\/マスコット>/g
s/#{FootX/#{footX/g
s/\"DashIeCeilingLeftEdgeFromJump\"/\"猛ダッシュでIEの天井の左の端っこから飛び降りる\"/g
s/\"DashIeCeilingRightEdgeFromJump\"/\"猛ダッシュでIEの天井の右の端っこから飛び降りる\"/g
s/\"DashIeCeilingLeftEdgeFromJump\"/\"猛ダッシュでIEの天井の左の端っこから飛び降りる\"/g
s/\"DashIeCeilingRightEdgeFromJump\"/\"猛ダッシュでIEの天井の右の端っこから飛び降りる\"/g
s/Gap=/ずれ=/g
s/+Gap/+ずれ/g
s/\"PullUpShimeji1\"/\"引っこ抜く1\"/g
s/BornX=/生まれる場所X=/g
s/BornY=/生まれる場所Y=/g
s/BornBehavior=/生まれた時の行動=/g
s/\"PullUpShimeji2\"/\"引っこ抜く2\"/g
s/\"Divide1\"/\"分裂1\"/g
