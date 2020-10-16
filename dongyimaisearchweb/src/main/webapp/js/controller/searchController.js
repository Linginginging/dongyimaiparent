app.controller('searchController',function ($scope,$location,searchService) {


    $scope.loadKeywords = function(){
        $scope.searchMap.keywords = $location.search()['keywords'];
        $scope.search();
    }

    $scope.searchMap = {
        keywords:'',
        category:'',
        brand:'',
        price:'',
        pageNo:1,
        pageSize:10,
        sort:'',
        sortFiled:'',
        spec:{}
    };

    //隐藏品牌
    $scope.keywordsIsBrand = function(){
        for(var i=0;i<$scope.resultMap.brandList.length;i++){
            if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text) >= 0){
                return true;
            }
        }
        return false;
    }

    //排序查询
    $scope.sortSearch = function(sortField,sortValue){
        $scope.searchMap.sort = sortValue;
        $scope.searchMap.sortFiled = sortField;
        $scope.search();
    }

    //判断指定页码是否是当前页
    $scope.ispage=function (p) {
        if(parseInt(p)==parseInt($scope.searchMap.pageNo)){
            return true;
        }else {
            return false;
        }
    }

    $scope.queryByPage = function(pageNo){

        if(pageNo < 0 || pageNo > $scope.resultMap.totalPages){
            return ;
        }

        $scope.searchMap.pageNo = pageNo;
        $scope.search();
    }

    buildPageLabel = function(){

        //取总页数当做页码的最后一页
        var lastPage = $scope.resultMap.totalPages;
        var firstPage = 1;

        $scope.firstDot = true;
        $scope.lastDot = true;

        //自定义 页码只显示5页
        if(lastPage > 5){
            //当前页 与 5 的关系
            if($scope.searchMap.pageNo < 5){
                lastPage = 5;
                $scope.firstDot = false;
            }else if($scope.searchMap.pageNo > (lastPage-2)) {
                firstPage = lastPage - 4;
                $scope.lastDot = false;
            }else{
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        }else{
            $scope.firstDot = false;
            $scope.lastDot = false;
        }

        $scope.pageLabel = [];

        for(var i=firstPage;i<=lastPage;i++){
            $scope.pageLabel.push(i);
        }

    }

    $scope.removeSearchMap = function(key){

        $scope.searchMap.pageNo=1;

        if('category'==key || 'brand'==key || 'price'==key){
            $scope.searchMap[key] = '';
        }else{
           delete $scope.searchMap.spec[key];
        }
        $scope.search();
    }

    $scope.addSearchMap = function(key,value){

        $scope.searchMap.pageNo=1;

        if('category'==key || 'brand'==key || 'price'==key){
            $scope.searchMap[key] = value;
        }else{
            $scope.searchMap.spec[key] = value;
        }
        $scope.search();
    }

    $scope.search = function () {

        $scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);

        searchService.search($scope.searchMap).success(
            function (response) {
                $scope.resultMap = response;
                //拼接分页页码
                buildPageLabel();
            }
        );
    }

})