const response = (statusCode: number, data: any) => {
  return {
    statusCode: statusCode,
    body: JSON.stringify(data),
  };
};

export default response;
