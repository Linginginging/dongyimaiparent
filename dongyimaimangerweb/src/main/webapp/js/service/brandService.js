app.service('brandService',function ($http) {

    this.delete = function (ids) {
        return $http.get('../brand/delete.do?ids='+ids);
    }

    this.findOne = function (id) {
        return $http.get('../brand/findOne.do?id='+id);
    }

    this.save = function (methodName,entity) {
        return $http.post('../brand/'+methodName+'.do',entity);
    }

    this.search = function (pageNum,pageSize,searchEntity) {
        return $http.post('../brand/search.do?pageNum='+pageNum+"&pageSize="+pageSize,searchEntity);
    }
    
    this.findAll = function () {
        return $http.get('../brand/selectOptionList.do');
    }

});