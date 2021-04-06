package com.example.kotlinchat

//class FriendlyMessage(aText: String?, aName: String, aPhotoUrl: String, aImageUrl: String?) {
//
//    var text: String? = aText
//    var name: String = aName
//    var photoUrl: String = aPhotoUrl
//    var imageUrl: String? = aImageUrl
//
//
//}
//

class FriendlyMessage {
    var text: String? = null
    var name: String? = null
    var photoUrl: String? = null
    var imageUrl: String? = null

    constructor(){}
    constructor(
        text: String?,
        name: String?,
        photoUrl: String?,
        imageUrl: String?
    ) {
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
        this.imageUrl = imageUrl
    }

}