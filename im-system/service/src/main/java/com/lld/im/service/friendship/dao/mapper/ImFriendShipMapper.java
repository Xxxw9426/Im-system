package com.lld.im.service.friendship.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lld.im.service.friendship.dao.ImFriendShipEntity;
import com.lld.im.service.friendship.model.req.CheckFriendShipReq;
import com.lld.im.service.friendship.model.resp.CheckFriendShipResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 关系链模块dao层
@Mapper
public interface ImFriendShipMapper extends BaseMapper<ImFriendShipEntity> {


    // 单向检验好友关系的dao层方法
    @Select("<script>"+
            "select from_id as fromId , to_id as toId , if(status = 1,1,0) as status from im_friendship where from_id =#{fromId} and to_id in"+
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='(' >"+
            "#{id}"+
            "</foreach>"+
            "</script>")
    public List<CheckFriendShipResp> checkFriendShip(CheckFriendShipReq req);


    // 双向检验好友关系的dao层方法
    @Select("<script>" +
            " select a.fromId,a.toId , ( \n" +
            " case \n" +
            " when a.status = 1 and b.status = 1 then 1 \n" +
            " when a.status = 1 and b.status != 1 then 2 \n" +
            " when a.status != 1 and b.status = 1 then 3 \n" +
            " when a.status != 1 and b.status != 1 then 4 \n" +
            " end \n" +
            " ) \n " +
            " as status from "+
            " (select from_id AS fromId , to_id AS toId , if(status = 1,1,0) as status from im_friendship where app_id = #{appId} and from_id = #{fromId} AND to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as a INNER join" +
            " (select from_id AS fromId, to_id AS toId , if(status = 1,1,0) as status from im_friendship where app_id = #{appId} and to_id = #{fromId} AND from_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as b " +
            " on a.fromId = b.toId AND b.fromId = a.toId "+
            "</script>"
    )
    public List<CheckFriendShipResp> checkFriendShipBoth(CheckFriendShipReq toId);


    // 单向检验黑名单的dao层方法
    @Select("<script>" +
            " select from_id AS fromId, to_id AS toId , if(black = 1,1,0) as status from im_friendship where app_id = #{appId} and from_id = #{fromId}  and  to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            "</script>"
    )
    public List<CheckFriendShipResp> checkFriendShipBlack(CheckFriendShipReq req);


    // 双向检验黑名单的dao层方法
    @Select("<script>" +
            " select a.fromId,a.toId , ( \n" +
            " case \n" +
            " when a.black = 1 and b.black = 1 then 1 \n" +
            " when a.black = 1 and b.black != 1 then 2 \n" +
            " when a.black != 1 and b.black = 1 then 3 \n" +
            " when a.black != 1 and b.black != 1 then 4 \n" +
            " end \n" +
            " ) \n " +
            " as status from "+
            " (select from_id AS fromId , to_id AS toId , if(black = 1,1,0) as black from im_friendship where app_id = #{appId} and from_id = #{fromId} AND to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as a INNER join" +
            " (select from_id AS fromId, to_id AS toId , if(black = 1,1,0) as black from im_friendship where app_id = #{appId} and to_id = #{fromId} AND from_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as b " +
            " on a.fromId = b.toId AND b.fromId = a.toId "+
            "</script>"
    )
    public List<CheckFriendShipResp> checkFriendShipBlackBoth(CheckFriendShipReq toId);


    @Select("select max(friend_sequence) from `im-core`.im_friendship where app_id = #{appId} AND from_id = #{userId}")
    public Long getFriendShipMaxSeq(Integer appId,String userId);


    @Select(
            " select to_id from im_friendship where from_id = #{userId} AND app_id = #{appId} and status = 1 and black = 1 "
    )
    List<String> getAllFriend(String userId,Integer appId);
}

