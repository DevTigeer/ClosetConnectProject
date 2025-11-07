INSERT INTO community_board (name, slug, type, visibility, is_system, sort_order, created_time)
VALUES
    ('자유게시판','free','FREE','PUBLIC',1,10,NOW()),
    ('물건게시판','item','ITEM','PUBLIC',1,20,NOW()),
    ('홍보게시판','promo','PROMO','PUBLIC',1,30,NOW()),
    ('OOTD게시판','ootd','OOTD','PUBLIC',1,40,NOW()),
    ('편집숍게시판','select','SELECT','PUBLIC',1,50,NOW()),
    ('중고거래게시판','market','MARKET','PUBLIC',1,60,NOW());
