jQuery(function($) {
    $('.modal-trigger').leanModal();

    $(".ami-badge").each(function(i, el){
        var badge = $(el);
        badge.css("cursor", "pointer");
        badge.click(function(){
            var modalId = badge.find(".modal-trigger").attr("href");
            $(modalId).openModal();
        });
    });
});
