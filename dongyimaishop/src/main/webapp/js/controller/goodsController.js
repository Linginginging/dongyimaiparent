 //控制层 
app.controller('goodsController' ,function($scope,$controller,$location,uploadService,typeTemplateService,goodsService,itemCatService){
	
	$controller('baseController',{$scope:$scope});//继承

	//模拟状态值
	$scope.status = ['未审核','审核通过','审核驳回','已关闭'];

    $scope.itemCatList = [];

	$scope.findItemCatList = function(){
        itemCatService.findAll().success(
        	function (response) {
        		for(var i=0;i<response.length;i++){
                    $scope.itemCatList[response[i].id] = response[i].name;
				}
            }
		);
	}

	$scope.entity = {
		goods:{},
		goodsDesc:{
			itemImages:[],
			specificationItems:[]
		},
        itemList:[]
	};

	$scope.createItemList = function(){
		//每一个产品对应的不同规格的列表
		$scope.entity.itemList = [{spec:{},price:0,num:99999,status:'0',isDefault:'0'}];
		//取所勾选的 规格
        //[{"attributeName":"机身内存","attributeValue":["16G","64G"]},{"attributeName":"网络","attributeValue":["移动3G","移动4G","联通3G"]}]
        var item = $scope.entity.goodsDesc.specificationItems;

		//遍历itemList 丰富规格
		for(var i=0;i<item.length;i++){
            //[{"attributeName":"机身内存","attributeValue":["16G","64G"]}]
            //[{spec:{'机身内存'：'16G'},price:0,num:99999,status:'0',isDefatule:'0'},{spec:{'机身内存'：'64G'},price:0,num:99999,status:'0',isDefatule:'0'}];

            // {"attributeName":"网络","attributeValue":["移动3G","移动4G","联通3G"]}
            $scope.entity.itemList = addColumn($scope.entity.itemList,item[i].attributeName,item[i].attributeValue);
		}

	}

	//对规格列表的拼接
	addColumn = function(list,columnName,columnValues){
		//方法的返回值 拼接好的每一条数据
		var newList = [];
        //[{spec:{},price:0,num:99999,status:'0',isDefatule:'0'}];
        //[{spec:{'机身内存'：'16G'},price:0,num:99999,status:'0',isDefatule:'0'},{spec:{'机身内存'：'64G'},price:0,num:99999,status:'0',isDefatule:'0'}];
		for(var i=0;i<list.length;i++){
            //克隆旧模板 模板中可能有多个对象 分别取出 分别规格拼接
            // {spec:{'机身内存'：'16G'},price:0,num:99999,status:'0',isDefatule:'0'}
            var oldRow = list[i];
			//采用JSON 的方法实现格式克隆 16G 64G
			//网络 [移动3G","移动4G","联通3G"]
			for(var j=0;j<columnValues.length;j++){
				//深克隆 json格式的字符串
                var newRow = JSON.parse(JSON.stringify(oldRow));
                //[{spec:{'机身内存'：'64G'},price:0,num:99999,status:'0',isDefatule:'0'}];
				//[{spec:{'机身内存'：'64G','网络'：'移动3G'},price:0,num:99999,status:'0',isDefatule:'0'}];
                newRow.spec[columnName] = columnValues[j];
                //[{spec:{'机身内存'：'16G'},price:0,num:99999,status:'0',isDefatule:'0'},{spec:{'机身内存'：'64G'},price:0,num:99999,status:'0',isDefatule:'0'}];
                // [{spec:{'机身内存'：'16G','网络'：'移动3G'},price:0,num:99999,status:'0',isDefatule:'0'},
				// {spec:{'机身内存'：'16G','网络'：'移动4G'},price:0,num:99999,status:'0',isDefatule:'0'},
                // {spec:{'机身内存'：'16G','网络'：'联通3G'},price:0,num:99999,status:'0',isDefatule:'0'},
				// {spec:{'机身内存'：'64G','网络'：'移动3G'},price:0,num:99999,status:'0',isDefatule:'0'}，
                // {spec:{'机身内存'：'64G','网络'：'移动4G'},price:0,num:99999,status:'0',isDefatule:'0'}，
				// {spec:{'机身内存'：'64G','网络'：'联通3G'},price:0,num:99999,status:'0',isDefatule:'0'}，
				// ];
                newList.push(newRow);
			}
		}
		return newList;
    }

    //修改方法中 验证规格是否存在 			网络 	移动3G
    $scope.checkAttributeValue = function(specName,optionName){
		//先取得原来选择过的规格 集合
        var specificationItems = $scope.entity.goodsDesc.specificationItems;
		//获取该规格 所选择过的 集合
        // {"attributeValue":["移动3G","移动4G"],"attributeName":"网络"},
        var obj = $scope.searchObjectByKey(specificationItems,'attributeName',specName);
        //判断集合中的元素是否被选择过 如果obj为空 说明该规格从未选择过 如果是返回true 勾选 如果是false 不勾选
		if(obj != null){
			//判断选择数组中 是否包含要放的规格选项
            // {"attributeValue":["移动3G","移动4G"],"attributeName":"网络"},
            // ["移动3G","移动4G"]
			if(obj.attributeValue.indexOf(optionName)>=0){
				return true;
			}else{
				return false;
			}

		}else{
			return false;
		}

	}

	//[{'attributeName':'网络','attributeValue':[]},{},{}...]
	//集合容器比较内容 比较头 判断是不是比较的网络 或其他头信息
    //     [{"attributeValue":["移动3G","移动4G"],"attributeName":"网络"},
	//     {"attributeValue":["128G","64G"],"attributeName":"机身内存"}]
	$scope.searchObjectByKey = function(list,key,value){
		//遍历容器
		for(var i=0;i<list.length;i++){
			if(list[i][key] == value){
				return list[i];
			}
		}
		return null;
	}

	//网络 移动3G [{'attributeName':'网络','attributeValue':[移动3G,移动4G]}]
	//修改规格的方法 $event复选框对象 name每一个规格属性名称 比如：网络 value属性值 比如：移动3G
	$scope.updateSepcification = function($event,name,value){
		//算法比较
		var obj = $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems,'attributeName',name);

		if(obj != null){
			//是否被勾选中
			if($event.target.checked){
				//被选中
                obj.attributeValue.push(value);
			}else{
				//没有被选中
                obj.attributeValue.splice(obj.attributeValue.indexOf(value),1);

                //如果删除规格选项已经是最后一个了 那么需要将规格对象移除
				if(obj.attributeValue.length == 0){
                    $scope.entity.goodsDesc.specificationItems.splice($scope.entity.goodsDesc.specificationItems.indexOf(obj),1);
				}
			}
		}else{
			//第一次点击的 数组内容为空
            //网络 移动3G [{'attributeName':'网络','attributeValue':[移动3G]}]
            $scope.entity.goodsDesc.specificationItems.push({'attributeName':name,'attributeValue':[value]});
		}
	}

	
	$scope.delete_images_entity = function(index){
		$scope.entity.goodsDesc.itemImages.splice(index,1);
	}

	//图片列表装载
	$scope.add_images_entity = function(){
		$scope.entity.goodsDesc.itemImages.push($scope.image_entity);
	}

    $scope.$watch('entity.goods.typeTemplateId',function (newValue) {
        typeTemplateService.findOne(newValue).success(
            function (response) {
                $scope.typeTemplate = response;
                $scope.typeTemplate.brandIds = JSON.parse($scope.typeTemplate.brandIds);
                //修改时id是有值的 不进行扩展属性 空串的覆盖
                if($location.search()['id']==null) {
                    $scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.typeTemplate.customAttributeItems);
                }
            }
        );

        typeTemplateService.findSpecById(newValue).success(
            function (response) {
                $scope.specList = response;
            }
        );

    });

    $scope.$watch('entity.goods.category3Id',function (newValue) {
		itemCatService.findOne(newValue).success(
			function (response) {
                $scope.entity.goods.typeTemplateId = response.typeId;
			}
		);
	});
    $scope.$watch('entity.goods.category2Id',function (newValue) {
        itemCatService.findItemCatByParentId(newValue).success(
            function (response) {
                $scope.itemCatList3 = response;
            }
        );
    });

	$scope.$watch('entity.goods.category1Id',function (newValue) {
        itemCatService.findItemCatByParentId(newValue).success(
            function (response) {
                $scope.itemCatList2 = response;
            }
        );
    });

	//查询一级标题
	$scope.findItemCatList1 = function(id){
		itemCatService.findItemCatByParentId(id).success(
			function (response) {
				$scope.itemCatList1 = response;
            }
		);
	}

	$scope.upload = function(){
		uploadService.upload().success(
			function (response) {
				if(response.success){
					$scope.image_entity.url = response.message;
				}else{
                    alert(response.message);
				}
            }
		).error(
			function () {
				alert("服务器发生错误");
            }
		);
	}

    //读取列表数据绑定到表单中  
	$scope.findAll=function(){
		goodsService.findAll().success(
			function(response){
				$scope.list=response;
			}			
		);
	}    
	
	//分页
	$scope.findPage=function(page,rows){			
		goodsService.findPage(page,rows).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	
	//查询实体 
	$scope.findOne=function(){

		var id = $location.search()['id'];

		if(id == null){
			return null;
		}

		goodsService.findOne(id).success(
			function(response){
				$scope.entity= response;

                editor.html($scope.entity.goodsDesc.introduction);

                $scope.entity.goodsDesc.itemImages = JSON.parse($scope.entity.goodsDesc.itemImages);
                $scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.entity.goodsDesc.customAttributeItems);
                $scope.entity.goodsDesc.specificationItems = JSON.parse($scope.entity.goodsDesc.specificationItems);

                //对sku 中的spec属性进行格式化转化
                for( var i=0;i<$scope.entity.itemList.length;i++ ){
                    $scope.entity.itemList[i].spec =
                        JSON.parse( $scope.entity.itemList[i].spec);
                }

            }
		);				
	}


	
	//保存 
	$scope.save=function(){

        $scope.entity.goodsDesc.introduction=editor.html();

		var serviceObject;//服务层对象  				
		if($scope.entity.goods.id!=null){//如果有ID
			serviceObject=goodsService.update( $scope.entity ); //修改  
		}else{
			serviceObject=goodsService.add( $scope.entity  );//增加 
		}				
		serviceObject.success(
			function(response){
				if(response.success){
					// //重新查询
		        	// $scope.reloadList();//重新加载
                    // editor.html('');
                    // $scope.entity={ goodsDesc:{itemImages:[],specificationItems:[]}};
                    location.href="goods.html";//跳转到商品列表页
				}else{
					alert(response.message);
				}
			}		
		);				
	}
	
	 
	//批量删除 
	$scope.dele=function(){			
		//获取选中的复选框			
		goodsService.dele( $scope.selectIds ).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新列表
					$scope.selectIds=[];
				}						
			}		
		);				
	}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		goodsService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
    
});	